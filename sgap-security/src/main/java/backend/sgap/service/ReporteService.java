package backend.sgap.service;

import backend.sgap.entity.Archivo;
import backend.sgap.enums.EstadoArchivo;
import backend.sgap.enums.NivelRiesgo;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de generación de reportes PDF para SGAP.
 *
 * <p>Utiliza <b>OpenPDF</b> (fork libre de iText 4) para generar PDFs en memoria
 * que son devueltos como {@code byte[]} al controlador.
 *
 * <h3>Reportes disponibles</h3>
 * <ul>
 *   <li>{@link #generarReporteArchivo(Archivo)} – reporte individual de un archivo analizado.</li>
 *   <li>{@link #generarReporteConsolidado(List)} – reporte resumen de todos los archivos.</li>
 * </ul>
 *
 * <h3>Dependencia Maven requerida (agregar a pom.xml)</h3>
 * <pre>{@code
 * <dependency>
 *     <groupId>com.github.librepdf</groupId>
 *     <artifactId>openpdf</artifactId>
 *     <version>1.3.43</version>
 * </dependency>
 * }</pre>
 */
@Service
@Slf4j
public class ReporteService {

    // ── Constantes de estilo ──────────────────────────────────────────────────

    private static final Color COLOR_PRIMARIO   = new Color(15, 23, 42);   // azul oscuro
    private static final Color COLOR_ALTO       = new Color(220, 38, 38);  // rojo
    private static final Color COLOR_MEDIO      = new Color(180, 83, 9);   // ámbar
    private static final Color COLOR_BAJO       = new Color(22, 163, 74);  // verde
    private static final Color COLOR_GRIS       = new Color(107, 114, 128);
    private static final Color COLOR_FONDO_TABLA= new Color(248, 250, 252);
    private static final Color COLOR_HEADER_TABLA= new Color(30, 41, 59);

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ── Fuentes ───────────────────────────────────────────────────────────────

    private Font fuenteTitulo()      { return new Font(Font.HELVETICA, 22, Font.BOLD,  COLOR_PRIMARIO); }
    private Font fuenteSubtitulo()   { return new Font(Font.HELVETICA, 13, Font.BOLD,  COLOR_PRIMARIO); }
    private Font fuenteEtiqueta()    { return new Font(Font.HELVETICA, 9,  Font.BOLD,  COLOR_GRIS);     }
    private Font fuenteValor()       { return new Font(Font.HELVETICA, 10, Font.NORMAL,COLOR_PRIMARIO); }
    private Font fuenteHeaderTabla() { return new Font(Font.HELVETICA, 9,  Font.BOLD,  Color.WHITE);    }
    private Font fuenteCeldaTabla()  { return new Font(Font.HELVETICA, 9,  Font.NORMAL,COLOR_PRIMARIO); }
    private Font fuentePequena()     { return new Font(Font.HELVETICA, 8,  Font.NORMAL,COLOR_GRIS);     }

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Genera el reporte PDF de análisis de seguridad para un archivo individual.
     *
     * @param archivo entidad con todos los datos del análisis
     * @return bytes del PDF generado en memoria
     * @throws Exception si la generación falla
     */
    public byte[] generarReporteArchivo(Archivo archivo) throws Exception {
        log.info("[REPORTE] Generando reporte individual para archivo id={}", archivo.getId());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 50, 50, 60, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        agregarMetadatos(doc, "Reporte de Análisis - " + archivo.getNombre());
        doc.open();

        // ── Encabezado ──
        agregarEncabezado(doc, writer, "Reporte de Análisis de Seguridad");
        doc.add(Chunk.NEWLINE);

        // ── Información básica del archivo ──
        doc.add(titulSeccion("Información del Archivo"));
        PdfPTable infoTable = crearTablaInfo(2);
        agregarFilaInfo(infoTable, "Nombre del Archivo", archivo.getNombre());
        agregarFilaInfo(infoTable, "ID en el Sistema",   String.valueOf(archivo.getId()));
        agregarFilaInfo(infoTable, "Tamaño",             formatSize(archivo.getTamano()));
        agregarFilaInfo(infoTable, "Fecha de Recepción", formatFecha(archivo.getFechaSubida()));
        agregarFilaInfo(infoTable, "Estado Actual",      estadoLabel(archivo.getEstado()));
        agregarFilaInfo(infoTable, "Hash SHA-256",       nvl(archivo.getHash()));
        doc.add(infoTable);
        doc.add(Chunk.NEWLINE);

        // ── Resultado del análisis de seguridad ──
        doc.add(titulSeccion("Resultado del Análisis de Seguridad"));
        PdfPTable analisisTable = crearTablaInfo(2);
        agregarFilaInfo(analisisTable, "MIME Declarado (cliente)",    nvl(archivo.getTipoArchivo()));
        agregarFilaInfo(analisisTable, "MIME Detectado (Apache Tika)",nvl(archivo.getMimeDetectado()));
        agregarFilaInfo(analisisTable, "Extensión Declarada",         nvl(archivo.getExtensionDeclarada()));
        agregarFilaRiesgo(analisisTable, archivo.getNivelRiesgo());
        doc.add(analisisTable);
        doc.add(Chunk.NEWLINE);

        // ── Flags de seguridad ──
        if (archivo.getFlagsSeguridad() != null && !archivo.getFlagsSeguridad().isBlank()) {
            doc.add(titulSeccion("Flags de Seguridad Detectados"));
            PdfPTable flagsTable = new PdfPTable(1);
            flagsTable.setWidthPercentage(100);
            String[] flags = archivo.getFlagsSeguridad().split(",");
            for (String flag : flags) {
                PdfPCell cell = new PdfPCell(new Phrase(flag.trim(), fuenteValor()));
                cell.setBackgroundColor(new Color(254, 226, 226));
                cell.setPadding(6);
                cell.setBorderColor(new Color(252, 165, 165));
                flagsTable.addCell(cell);
            }
            doc.add(flagsTable);
            doc.add(Chunk.NEWLINE);
        }

        // ── Descripción del análisis ──
        if (archivo.getDescripcionAnalisis() != null && !archivo.getDescripcionAnalisis().isBlank()) {
            doc.add(titulSeccion("Descripción Detallada del Análisis"));
            Paragraph desc = new Paragraph(archivo.getDescripcionAnalisis(), fuenteValor());
            desc.setLeading(14);
            desc.setSpacingBefore(4);
            doc.add(desc);
            doc.add(Chunk.NEWLINE);
        }

        // ── Recomendaciones ──
        doc.add(titulSeccion("Recomendaciones"));
        doc.add(recomendaciones(archivo.getNivelRiesgo()));

        // ── Pie de página ──
        agregarPiePagina(doc, writer);

        doc.close();
        log.info("[REPORTE] PDF individual generado ({} bytes)", baos.size());
        return baos.toByteArray();
    }

    /**
     * Genera el reporte PDF consolidado con el resumen de todos los archivos.
     *
     * @param archivos lista completa de archivos del repositorio
     * @return bytes del PDF generado en memoria
     * @throws Exception si la generación falla
     */
    public byte[] generarReporteConsolidado(List<Archivo> archivos) throws Exception {
        log.info("[REPORTE] Generando reporte consolidado ({} archivos)", archivos.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 40, 40, 60, 40); // apaisado para la tabla
        PdfWriter writer = PdfWriter.getInstance(doc, baos);

        agregarMetadatos(doc, "Reporte Consolidado SGAP");
        doc.open();

        // ── Encabezado ──
        agregarEncabezado(doc, writer, "Reporte Consolidado de Seguridad");
        doc.add(Chunk.NEWLINE);

        // ── Estadísticas generales ──
        doc.add(titulSeccion("Estadísticas Generales"));
        doc.add(tablaEstadisticas(archivos));
        doc.add(Chunk.NEWLINE);

        // ── Tabla de archivos ──
        doc.add(titulSeccion("Detalle de Archivos Registrados (" + archivos.size() + " en total)"));
        doc.add(tablaArchivos(archivos));

        // ── Pie ──
        agregarPiePagina(doc, writer);

        doc.close();
        log.info("[REPORTE] PDF consolidado generado ({} bytes)", baos.size());
        return baos.toByteArray();
    }

    // ── Helpers de construcción ───────────────────────────────────────────────

    private void agregarMetadatos(Document doc, String titulo) {
        doc.addTitle(titulo);
        doc.addAuthor("SGAP – Sistema de Gestión y Análisis de Archivos Peligrosos");
        doc.addCreationDate();
    }

    private void agregarEncabezado(Document doc, PdfWriter writer, String titulo) throws DocumentException {
        // Barra de color en la parte superior
        PdfContentByte cb = writer.getDirectContent();
        cb.setColorFill(COLOR_PRIMARIO);
        cb.rectangle(doc.left(), doc.top() + 10, doc.right() - doc.left(), 8);
        cb.fill();

        // Logo/título
        Paragraph sgap = new Paragraph("SGAP", fuenteTitulo());
        sgap.setSpacingBefore(10);
        doc.add(sgap);

        Paragraph subtitulo = new Paragraph(titulo, new Font(Font.HELVETICA, 14, Font.NORMAL, COLOR_GRIS));
        subtitulo.setSpacingAfter(4);
        doc.add(subtitulo);

        Paragraph fecha = new Paragraph(
                "Generado el " + LocalDateTime.now().format(FMT),
                fuentePequena());
        doc.add(fecha);

        // Línea separadora
        LineSeparator line = new LineSeparator(1, 100, COLOR_PRIMARIO, Element.ALIGN_LEFT, -2);
        doc.add(new Chunk(line));
    }

    private Paragraph titulSeccion(String texto) {
        Paragraph p = new Paragraph(texto, fuenteSubtitulo());
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        return p;
    }

    private PdfPTable crearTablaInfo(int columnas) throws DocumentException {
        PdfPTable table = new PdfPTable(columnas * 2);
        table.setWidthPercentage(100);
        float[] anchos = new float[columnas * 2];
        for (int i = 0; i < columnas * 2; i++) {
            anchos[i] = (i % 2 == 0) ? 1.2f : 2f;
        }
        table.setWidths(anchos);
        return table;
    }

    private void agregarFilaInfo(PdfPTable table, String etiqueta, String valor) {
        PdfPCell celEtiqueta = new PdfPCell(new Phrase(etiqueta, fuenteEtiqueta()));
        celEtiqueta.setBackgroundColor(COLOR_FONDO_TABLA);
        celEtiqueta.setPadding(6);
        celEtiqueta.setBorderColor(new Color(229, 231, 235));

        PdfPCell celValor = new PdfPCell(new Phrase(valor, fuenteValor()));
        celValor.setPadding(6);
        celValor.setBorderColor(new Color(229, 231, 235));

        table.addCell(celEtiqueta);
        table.addCell(celValor);
    }

    private void agregarFilaRiesgo(PdfPTable table, NivelRiesgo nivel) {
        PdfPCell celEtiqueta = new PdfPCell(new Phrase("Nivel de Riesgo", fuenteEtiqueta()));
        celEtiqueta.setBackgroundColor(COLOR_FONDO_TABLA);
        celEtiqueta.setPadding(6);
        celEtiqueta.setBorderColor(new Color(229, 231, 235));

        Color colorRiesgo = colorNivel(nivel);
        PdfPCell celValor = new PdfPCell(new Phrase(riesgoLabel(nivel),
                new Font(Font.HELVETICA, 10, Font.BOLD, colorRiesgo)));
        celValor.setPadding(6);
        celValor.setBorderColor(new Color(229, 231, 235));

        table.addCell(celEtiqueta);
        table.addCell(celValor);
    }

    private PdfPTable tablaEstadisticas(List<Archivo> archivos) throws DocumentException {
        long alto  = archivos.stream().filter(a -> a.getNivelRiesgo() == NivelRiesgo.HIGH).count();
        long medio = archivos.stream().filter(a -> a.getNivelRiesgo() == NivelRiesgo.MEDIUM).count();
        long bajo  = archivos.stream().filter(a -> a.getNivelRiesgo() == NivelRiesgo.LOW).count();

        Map<EstadoArchivo, Long> porEstado = archivos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getEstado() != null ? a.getEstado() : EstadoArchivo.PENDING,
                        Collectors.counting()));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        String[] headers = { "Total Archivos", "Riesgo Alto", "Riesgo Medio", "Riesgo Bajo" };
        String[] values  = {
                String.valueOf(archivos.size()),
                String.valueOf(alto),
                String.valueOf(medio),
                String.valueOf(bajo)
        };
        Color[] colors = { COLOR_PRIMARIO, COLOR_ALTO, COLOR_MEDIO, COLOR_BAJO };

        for (int i = 0; i < 4; i++) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(12);
            cell.setBorderColor(new Color(229, 231, 235));

            Paragraph label = new Paragraph(headers[i], fuenteEtiqueta());
            Paragraph value = new Paragraph(values[i],
                    new Font(Font.HELVETICA, 24, Font.BOLD, colors[i]));
            cell.addElement(label);
            cell.addElement(value);
            table.addCell(cell);
        }
        return table;
    }

    private PdfPTable tablaArchivos(List<Archivo> archivos) throws DocumentException {
        String[] headers = { "ID", "Nombre", "MIME Detectado", "Extensión", "Nivel Riesgo", "Estado", "Fecha" };
        float[]  anchos  = { 0.5f, 2.5f, 2f, 1f, 1.2f, 1.2f, 1.5f };

        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(anchos);

        // Cabecera
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fuenteHeaderTabla()));
            cell.setBackgroundColor(COLOR_HEADER_TABLA);
            cell.setPadding(7);
            cell.setBorderColor(COLOR_HEADER_TABLA);
            table.addCell(cell);
        }

        // Filas
        boolean alterno = false;
        for (Archivo a : archivos) {
            Color bg = alterno ? COLOR_FONDO_TABLA : Color.WHITE;
            alterno = !alterno;

            agregarCeldaTabla(table, String.valueOf(a.getId()), bg);
            agregarCeldaTabla(table, nvl(a.getNombre()), bg);
            agregarCeldaTabla(table, nvl(a.getMimeDetectado()), bg);
            agregarCeldaTabla(table, nvl(a.getExtensionDeclarada()), bg);

            // Celda de riesgo con color
            PdfPCell celRiesgo = new PdfPCell(new Phrase(
                    riesgoLabel(a.getNivelRiesgo()),
                    new Font(Font.HELVETICA, 8, Font.BOLD, colorNivel(a.getNivelRiesgo()))));
            celRiesgo.setBackgroundColor(bg);
            celRiesgo.setPadding(5);
            celRiesgo.setBorderColor(new Color(229, 231, 235));
            table.addCell(celRiesgo);

            agregarCeldaTabla(table, estadoLabel(a.getEstado()), bg);
            agregarCeldaTabla(table, formatFecha(a.getFechaSubida()), bg);
        }

        return table;
    }

    private void agregarCeldaTabla(PdfPTable table, String texto, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fuenteCeldaTabla()));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(new Color(229, 231, 235));
        table.addCell(cell);
    }

    private Paragraph recomendaciones(NivelRiesgo nivel) {
        StringBuilder sb = new StringBuilder();
        if (nivel == NivelRiesgo.HIGH) {
            sb.append("⚠ ACCIÓN INMEDIATA REQUERIDA\n\n")
              .append("• El archivo presenta características propias de software malicioso o ejecutable peligroso.\n")
              .append("• Mantener el archivo en cuarentena y no distribuirlo bajo ningún concepto.\n")
              .append("• Escalar a la dirección de seguridad para análisis forense completo.\n")
              .append("• Revisar los sistemas de origen para posible compromiso.\n")
              .append("• Documentar el incidente en el registro de seguridad.");
        } else if (nivel == NivelRiesgo.MEDIUM) {
            sb.append("⚠ REVISIÓN REQUERIDA\n\n")
              .append("• El archivo presenta indicadores que requieren revisión manual.\n")
              .append("• Verificar la procedencia y el propósito del archivo con el remitente.\n")
              .append("• No abrir ni ejecutar el archivo hasta completar la revisión.\n")
              .append("• Documentar la decisión tomada tras la revisión.");
        } else {
            sb.append("✓ MONITOREO ESTÁNDAR\n\n")
              .append("• El archivo no presentó indicadores de riesgo significativos en el análisis automático.\n")
              .append("• Continuar con el flujo de trabajo habitual.\n")
              .append("• Registrar cualquier comportamiento inusual durante su uso.");
        }
        Paragraph p = new Paragraph(sb.toString(), fuenteValor());
        p.setLeading(16);
        return p;
    }

    private void agregarPiePagina(Document doc, PdfWriter writer) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        LineSeparator line = new LineSeparator(1, 100, COLOR_GRIS, Element.ALIGN_LEFT, -2);
        doc.add(new Chunk(line));
        Paragraph pie = new Paragraph(
                "SGAP – Sistema de Gestión y Análisis de Archivos Peligrosos  |  " +
                "Reporte generado el " + LocalDateTime.now().format(FMT) +
                "  |  Documento confidencial – solo para personal autorizado",
                fuentePequena());
        pie.setSpacingBefore(6);
        doc.add(pie);
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private String nvl(String s)     { return s != null ? s : "—"; }
    private String formatSize(Long b) {
        if (b == null) return "—";
        if (b < 1024)    return b + " B";
        if (b < 1048576) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1048576.0);
    }
    private String formatFecha(LocalDateTime dt) {
        if (dt == null) return "—";
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    private String estadoLabel(EstadoArchivo e) {
        if (e == null) return "—";
        return switch (e) {
            case PENDING     -> "Pendiente";
            case PROCESSING  -> "Procesando";
            case SAFE        -> "Seguro";
            case QUARANTINED -> "En cuarentena";
            case UNSAFE      -> "Inseguro";
            case FAILED      -> "Fallido";
        };
    }
    private String riesgoLabel(NivelRiesgo n) {
        if (n == null) return "—";
        return switch (n) {
            case HIGH   -> "ALTO";
            case MEDIUM -> "MEDIO";
            case LOW    -> "BAJO";
        };
    }
    private Color colorNivel(NivelRiesgo n) {
        if (n == null) return COLOR_GRIS;
        return switch (n) {
            case HIGH   -> COLOR_ALTO;
            case MEDIUM -> COLOR_MEDIO;
            case LOW    -> COLOR_BAJO;
        };
    }
}
