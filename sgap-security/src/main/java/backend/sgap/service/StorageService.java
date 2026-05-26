package backend.sgap.service;

import io.minio.*;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/**
 * Servicio de almacenamiento de objetos sobre MinIO.
 *
 * <h3>Buckets usados</h3>
 * <pre>
 *   archivos/   ← archivos seguros (riesgo LOW)
 *   quarantine/ ← archivos de riesgo MEDIUM pendientes de revisión
 *   blocked/    ← archivos de riesgo HIGH bloqueados permanentemente
 * </pre>
 *
 * <p>Al arrancar verifica la existencia del bucket principal configurado y
 * crea automáticamente los tres prefijos si no existen (en MinIO los prefijos
 * son virtuales; los buckets sí se crean explícitamente).
 *
 * <p>Si {@code minio.public-url} está definido se construye una URL pública
 * directa; de lo contrario se genera una URL pre-firmada con validez de 7 días.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.public-url:}")
    private String publicBase;

    // ── Inicialización ────────────────────────────────────────────────────

    @PostConstruct
    public void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("[STORAGE] Bucket creado: {}", bucket);
        } else {
            log.info("[STORAGE] Bucket existente: {}", bucket);
        }
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Sube un archivo desde un {@link MultipartFile} al bucket configurado.
     *
     * @deprecated Preferir {@link #uploadFromPath(String, Path, String)} cuando
     *             el archivo ya está en cuarentena en disco (evita cargar el
     *             MultipartFile dos veces en memoria).
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public String upload(String objectKey, MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        log.info("[STORAGE] Subido (MultipartFile): {}/{}", bucket, objectKey);
        return resolveUrl(objectKey);
    }

    /**
     * Sube un archivo desde una ruta en disco (archivo en cuarentena).
     *
     * <p>Usa el MIME detectado por Tika como Content-Type real.
     * Nunca usa el Content-Type declarado por el cliente.
     *
     * @param objectKey     clave de objeto en MinIO (incluye prefijo de destino)
     * @param quarantinePath ruta local al archivo en cuarentena
     * @param mimeDetectado  MIME real detectado por Tika
     */
    public void uploadFromPath(String objectKey, Path quarantinePath, String mimeDetectado)
            throws Exception {
        long fileSize = Files.size(quarantinePath);
        try (InputStream is = Files.newInputStream(quarantinePath)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(is, fileSize, -1)
                    .contentType(mimeDetectado)   // MIME real, no el del cliente
                    .build());
        }
        log.info("[STORAGE] Subido desde cuarentena: {}/{} ({} bytes, mime={})",
                bucket, objectKey, fileSize, mimeDetectado);
    }

    /**
     * Genera una URL de acceso para el objeto dado.
     *
     * <p>Si {@code minio.public-url} está configurado devuelve URL pública;
     * de lo contrario genera una URL pre-firmada válida por 7 días.
     *
     * @param objectKey clave del objeto en MinIO
     * @return URL de acceso al objeto
     */
    public String getPresignedUrl(String objectKey) throws Exception {
        if (publicBase != null && !publicBase.isBlank()) {
            return publicBase + "/" + bucket + "/" + objectKey;
        }
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .method(Method.GET)
                .expiry((int) Duration.ofDays(7).toSeconds())
                .build());
    }

    /**
     * Construye una clave de objeto única bajo el prefijo indicado.
     *
     * <p>Prefijos estándar:
     * <ul>
     *   <li>{@code archivos}   – destino de archivos seguros</li>
     *   <li>{@code quarantine} – archivos de riesgo MEDIUM</li>
     *   <li>{@code blocked}    – archivos de riesgo HIGH</li>
     * </ul>
     *
     * <p>Ejemplo de resultado: {@code archivos/3f2a1b4c-...-uuid.pdf}
     *
     * @param prefix          prefijo del bucket ("archivos", "quarantine", "blocked")
     * @param originalFilename nombre original del archivo (puede ser null)
     * @return clave de objeto lista para usar en {@link #uploadFromPath}
     */
    public String buildObjectKey(String prefix, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            // Tomar solo la última extensión declarada (ignorar dobles extensiones)
            ext = "." + originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
        }
        return prefix + "/" + UUID.randomUUID() + ext;
    }

    /**
     * Sobrecarga de compatibilidad con el código original.
     * Usa el prefijo "archivos" por defecto.
     *
     * @deprecated Usar {@link #buildObjectKey(String, String)} con prefijo explícito.
     */
    @Deprecated(since = "2.0", forRemoval = false)
    public String buildObjectKey(String originalFilename) {
        return buildObjectKey("archivos", originalFilename);
    }

    // ── Helpers privados ──────────────────────────────────────────────────

    private String resolveUrl(String objectKey) throws Exception {
        return getPresignedUrl(objectKey);
    }
}
