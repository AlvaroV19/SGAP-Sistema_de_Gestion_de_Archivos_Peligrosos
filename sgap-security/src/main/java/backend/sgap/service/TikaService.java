package backend.sgap.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Servicio de detección de MIME real mediante Apache Tika.
 *
 * <p><strong>Principio fundamental:</strong> nunca confiar en el Content-Type
 * enviado por el cliente ni en la extensión del nombre de archivo. Tika analiza
 * los magic bytes y la estructura interna del archivo para determinar el tipo real.
 *
 * <p>La instancia de {@link Tika} es thread-safe y se reutiliza durante toda
 * la vida de la aplicación (no crear una nueva por petición).
 *
 * <h3>Ejemplos de detección</h3>
 * <pre>
 *   archivo.pdf  con contenido EXE  → "application/x-dosexec"   (mismatch)
 *   imagen.png   con contenido PNG  → "image/png"               (OK)
 *   script.sh    con shebang        → "text/x-shellscript"      (peligroso)
 *   backup.tar.gz                   → "application/x-gzip"      (OK)
 * </pre>
 */
@Service
@Slf4j
public class TikaService {

    /**
     * Tika es thread-safe; una sola instancia por aplicación es suficiente.
     * Usa detección por magic bytes + heurística de estructura.
     */
    private final Tika tika = new Tika();

    /**
     * Detecta el MIME real de un archivo en disco.
     * Lee solo los primeros bytes necesarios (eficiente).
     *
     * @param file ruta al archivo (debe existir y ser legible)
     * @return MIME type real según Tika (nunca null)
     * @throws IOException si el archivo no se puede leer
     */
    public String detectMime(Path file) throws IOException {
        String mime = tika.detect(file);
        log.debug("Tika MIME detection: {} → {}", file.getFileName(), mime);
        return mime;
    }

    /**
     * Detecta el MIME real desde un InputStream, opcionalmente ayudado
     * por el nombre del archivo (Tika lo usa como pista secundaria, no primaria).
     *
     * <p>Usar cuando no se tiene el archivo en disco.
     * <strong>Nota:</strong> el stream debe soportar {@code mark/reset}
     * para que Tika pueda leer los magic bytes sin consumir el stream.
     * Si no lo soporta, envolver con {@code BufferedInputStream}.
     *
     * @param inputStream stream del archivo (preferiblemente BufferedInputStream)
     * @param filename    nombre del archivo (pista secundaria, puede ser null)
     * @return MIME type real según Tika
     * @throws IOException si el stream no se puede leer
     */
    public String detectMime(InputStream inputStream, String filename) throws IOException {
        String mime = tika.detect(inputStream, filename);
        log.debug("Tika MIME detection (stream): {} → {}", filename, mime);
        return mime;
    }
}
