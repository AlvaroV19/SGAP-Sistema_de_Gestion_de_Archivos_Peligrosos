package backend.sgap.service;

import backend.sgap.dto.AnalisisResultado;
import backend.sgap.enums.NivelRiesgo;
import backend.sgap.enums.SecurityFlag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.*;

/**
 * Motor de análisis de riesgo de archivos.
 *
 * <p>Aplica un conjunto de reglas estáticas para calcular el nivel de riesgo
 * y los flags de seguridad de un archivo a partir de:
 * <ul>
 *   <li>El nombre original del archivo (extensión declarada).</li>
 *   <li>El MIME detectado por Apache Tika (fuente de verdad).</li>
 * </ul>
 *
 * <h3>Tabla de reglas activas</h3>
 * <pre>
 *  Regla                          │ Flag                  │ Riesgo
 *  ───────────────────────────────┼───────────────────────┼────────
 *  Extensión ejecutable/script    │ SUSPICIOUS_EXTENSION  │ HIGH
 *  MIME peligroso (Tika)          │ DANGEROUS_MIME        │ HIGH
 *  MIME incompatible con extensión│ MIME_MISMATCH         │ HIGH
 *  Doble extensión real           │ DOUBLE_EXTENSION      │ MEDIUM
 * </pre>
 *
 * <h3>Extensión futura</h3>
 * Los flags {@code YARA_MATCH}, {@code ANTIVIRUS_HIT}, {@code VIRUSTOTAL_MATCH}
 * y {@code HEURISTIC_ANOMALY} están reservados en el enum {@link SecurityFlag}
 * para su integración posterior.
 *
 * <p><strong>IMPORTANTE:</strong> nunca se confía en el Content-Type ni en
 * la extensión declarada por el cliente. El MIME de Tika es la única fuente
 * de verdad para decisiones de seguridad.
 */
@Service
@Slf4j
public class SecurityAnalysisService {

    // ── Configuración de reglas ───────────────────────────────────────────

    /**
     * Extensiones inherentemente ejecutables o de scripts.
     * Cualquier archivo con estas extensiones se marca HIGH directamente.
     */
    private static final Set<String> SUSPICIOUS_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "bash", "zsh", "fish",
            "deb", "rpm", "appimage",
            "jar", "war", "ear",
            "msi", "msix", "dll", "sys", "drv",
            "com", "pif", "scr", "hta",
            "ps1", "psm1", "psd1",   // PowerShell
            "vbs", "vbe", "js", "jse", "wsf", "wsh",  // Windows Script
            "py", "rb", "pl"         // scripts interpretados (contexto-dependiente)
    );

    /**
     * Extensiones compuestas legítimas que NO deben disparar la regla de doble extensión.
     */
    private static final Set<String> COMPOSITE_EXTENSION_WHITELIST = Set.of(
            "tar.gz", "tar.bz2", "tar.xz", "tar.zst", "tar.lz4"
    );

    /**
     * MIMEs que Tika puede detectar y que son inherentemente ejecutables o peligrosos,
     * independientemente de la extensión declarada.
     */
    private static final Set<String> DANGEROUS_MIMES = Set.of(
            "application/x-dosexec",           // PE ejecutable (EXE, DLL, SCR)
            "application/x-executable",        // ELF (Linux/macOS ejecutable)
            "application/x-sharedlib",         // .so / .dll
            "application/x-sh",                // shell script
            "application/x-shellscript",       // shell script (variante)
            "application/x-msdos-program",     // .com / .bat
            "application/x-msdownload",        // ejecutable Windows
            "application/java-archive",        // .jar ejecutable
            "application/x-python-code",       // bytecode Python compilado
            "application/x-ruby"               // script Ruby
    );

    /**
     * Mapa de compatibilidad extensión → prefijos de MIME esperados.
     * Si la extensión está en este mapa pero el MIME de Tika no coincide
     * con ningún prefijo esperado, se considera MIME_MISMATCH.
     *
     * <p>Se usan prefijos (no valores exactos) para absorber variantes:
     * ej. "image/jpeg" y "image/jpg" ambos empiezan con "image/jpeg"... bueno,
     * solo el primero, pero la lógica de {@code startsWith} cubre subcasos.
     */
    private static final Map<String, List<String>> MIME_COMPATIBILITY;

    static {
        Map<String, List<String>> map = new HashMap<>();
        // Documentos
        map.put("pdf",  List.of("application/pdf"));
        map.put("docx", List.of("application/vnd.openxmlformats-officedocument", "application/zip"));
        map.put("xlsx", List.of("application/vnd.openxmlformats-officedocument", "application/zip"));
        map.put("pptx", List.of("application/vnd.openxmlformats-officedocument", "application/zip"));
        map.put("doc",  List.of("application/msword"));
        map.put("xls",  List.of("application/vnd.ms-excel"));
        map.put("odt",  List.of("application/vnd.oasis"));
        // Imágenes
        map.put("png",  List.of("image/png"));
        map.put("jpg",  List.of("image/jpeg"));
        map.put("jpeg", List.of("image/jpeg"));
        map.put("gif",  List.of("image/gif"));
        map.put("webp", List.of("image/webp"));
        map.put("svg",  List.of("image/svg"));
        map.put("bmp",  List.of("image/bmp", "image/x-bmp"));
        map.put("ico",  List.of("image/x-icon", "image/vnd.microsoft.icon"));
        // Audio/Video
        map.put("mp4",  List.of("video/mp4"));
        map.put("mp3",  List.of("audio/mpeg"));
        map.put("wav",  List.of("audio/wav", "audio/x-wav"));
        map.put("ogg",  List.of("audio/ogg", "video/ogg"));
        // Comprimidos
        map.put("zip",  List.of("application/zip", "application/x-zip"));
        map.put("gz",   List.of("application/gzip", "application/x-gzip"));
        map.put("tar",  List.of("application/x-tar"));
        map.put("rar",  List.of("application/x-rar", "application/vnd.rar"));
        map.put("7z",   List.of("application/x-7z-compressed"));
        // Texto / datos
        map.put("txt",  List.of("text/"));
        map.put("csv",  List.of("text/", "application/csv"));
        map.put("json", List.of("application/json", "text/"));
        map.put("xml",  List.of("application/xml", "text/xml"));
        map.put("html", List.of("text/html"));
        map.put("md",   List.of("text/"));
        MIME_COMPATIBILITY = Collections.unmodifiableMap(map);
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Analiza un archivo y devuelve el resultado completo del motor de riesgo.
     *
     * @param filename     nombre original del archivo (declarado por el cliente)
     * @param mimeDetectado MIME detectado por Apache Tika sobre el contenido real
     * @return resultado del análisis con nivel de riesgo, flags y descripción
     */
    public AnalisisResultado analyze(String filename, String mimeDetectado) {
        String extension = extractExtension(filename).toLowerCase(Locale.ROOT);
        Set<SecurityFlag> flags = new HashSet<>();
        NivelRiesgo nivelRiesgo = NivelRiesgo.LOW;
        List<String> reasons = new ArrayList<>();

        // ── Regla 1: Extensión sospechosa ────────────────────────────────
        if (SUSPICIOUS_EXTENSIONS.contains(extension)) {
            flags.add(SecurityFlag.SUSPICIOUS_EXTENSION);
            nivelRiesgo = elevate(nivelRiesgo, NivelRiesgo.HIGH);
            reasons.add("Extensión sospechosa detectada: '." + extension + "'");
            log.warn("[SECURITY] SUSPICIOUS_EXTENSION | file='{}' ext='{}'", filename, extension);
        }

        // ── Regla 2: MIME peligroso detectado por Tika ───────────────────
        if (mimeDetectado != null && DANGEROUS_MIMES.contains(mimeDetectado)) {
            flags.add(SecurityFlag.DANGEROUS_MIME);
            nivelRiesgo = elevate(nivelRiesgo, NivelRiesgo.HIGH);
            reasons.add("MIME ejecutable detectado por Tika: '" + mimeDetectado + "'");
            log.warn("[SECURITY] DANGEROUS_MIME | file='{}' mime='{}'", filename, mimeDetectado);
        }

        // ── Regla 3: MIME incompatible con la extensión declarada ─────────
        if (isMimeMismatch(extension, mimeDetectado)) {
            flags.add(SecurityFlag.MIME_MISMATCH);
            nivelRiesgo = elevate(nivelRiesgo, NivelRiesgo.HIGH);
            reasons.add("MIME incompatible: extensión '." + extension
                    + "' pero Tika detectó '" + mimeDetectado + "'");
            log.warn("[SECURITY] MIME_MISMATCH | file='{}' ext='{}' tika='{}'",
                    filename, extension, mimeDetectado);
        }

        // ── Regla 4: Doble extensión real ────────────────────────────────
        if (hasDoubleExtension(filename)) {
            flags.add(SecurityFlag.DOUBLE_EXTENSION);
            nivelRiesgo = elevate(nivelRiesgo, NivelRiesgo.MEDIUM);
            reasons.add("Doble extensión detectada en nombre: '" + filename + "'");
            log.warn("[SECURITY] DOUBLE_EXTENSION | file='{}'", filename);
        }

        // ── Placeholder para motores futuros ──────────────────────────────
        // TODO: invocar YaraService.scan(path)     → flag YARA_MATCH      HIGH
        // TODO: invocar AntivirusService.scan(path) → flag ANTIVIRUS_HIT   HIGH
        // TODO: invocar VirusTotalService.check(hash) → flag VIRUSTOTAL_MATCH HIGH
        // TODO: invocar HeuristicService.analyze(path) → flag HEURISTIC_ANOMALY MEDIUM/HIGH

        String descripcion = reasons.isEmpty()
                ? "Sin anomalías detectadas"
                : String.join("; ", reasons);

        log.info("[SECURITY] Análisis completado | file='{}' risk={} flags={} mime='{}'",
                filename, nivelRiesgo, flags, mimeDetectado);

        return AnalisisResultado.builder()
                .mimeDetectado(mimeDetectado)
                .extensionDeclarada(extension)
                .nivelRiesgo(nivelRiesgo)
                .flags(Collections.unmodifiableSet(flags))
                .descripcion(descripcion)
                .build();
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    /**
     * Eleva el nivel de riesgo si el candidato es mayor al actual.
     * Garantiza que el riesgo solo puede subir, nunca bajar.
     */
    private NivelRiesgo elevate(NivelRiesgo current, NivelRiesgo candidate) {
        return candidate.ordinal() > current.ordinal() ? candidate : current;
    }

    /**
     * Extrae la extensión del nombre de archivo (sin punto, en minúsculas).
     * Devuelve cadena vacía si no hay extensión.
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * Detecta doble extensión real.
     *
     * <p>Ejemplos que SÍ disparan la regla:
     * <pre>
     *   factura.pdf.exe   → ["factura", "pdf", "exe"] → 3 partes → doble extensión
     *   imagen.png.sh     → doble extensión
     * </pre>
     *
     * <p>Ejemplos que NO disparan la regla (whitelist):
     * <pre>
     *   backup.tar.gz     → compuesto legítimo en COMPOSITE_EXTENSION_WHITELIST
     *   archivo.pdf       → solo 2 partes, una sola extensión
     * </pre>
     */
    private boolean hasDoubleExtension(String filename) {
        if (filename == null) return false;
        // Tomar solo el nombre base (sin ruta)
        String baseName = Paths.get(filename).getFileName().toString();
        String[] parts = baseName.split("\\.");
        // Necesitamos al menos "nombre + ext1 + ext2" → 3 segmentos
        if (parts.length < 3) return false;

        // Verificar whitelist de extensiones compuestas legítimas
        String lastTwo = parts[parts.length - 2].toLowerCase(Locale.ROOT)
                + "." + parts[parts.length - 1].toLowerCase(Locale.ROOT);
        if (COMPOSITE_EXTENSION_WHITELIST.contains(lastTwo)) return false;

        return true;
    }

    /**
     * Determina si el MIME detectado por Tika es incompatible con la extensión declarada.
     *
     * <p>Solo evalúa extensiones presentes en {@link #MIME_COMPATIBILITY}.
     * Extensiones desconocidas no se marcan como mismatch (insuficiente información).
     */
    private boolean isMimeMismatch(String extension, String mimeDetectado) {
        if (extension.isBlank() || mimeDetectado == null || mimeDetectado.isBlank()) return false;
        List<String> expectedPrefixes = MIME_COMPATIBILITY.get(extension);
        if (expectedPrefixes == null) return false; // extensión desconocida, no podemos concluir
        return expectedPrefixes.stream().noneMatch(mimeDetectado::startsWith);
    }
}
