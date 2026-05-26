package backend.sgap.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servicio encargado de agendar el procesamiento diferido de archivos.
 *
 * <p>Utiliza un {@link ScheduledExecutorService} para programar la ejecución del
 * pipeline de seguridad con la demora configurada en {@code file.processing.delay-ms}.
 *
 * <p>La responsabilidad de este servicio es exclusivamente <em>agendar</em> la tarea.
 * La ejecución real del pipeline queda delegada en {@link ArchivoProcessingService},
 * que corre en el pool {@code fileProcessingExecutor} con {@code @Async}.
 *
 * <h3>Por qué ScheduledExecutorService y no @Scheduled</h3>
 * <ul>
 *   <li>{@code @Scheduled} es para tareas periódicas con intervalo fijo. Aquí cada
 *       archivo tiene su propia demora dinámica que empieza en el momento del upload.</li>
 *   <li>{@code ScheduledExecutorService.schedule()} permite fijar una demora exacta
 *       para cada tarea individualmente, sin contaminación de estado.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelayedProcessingService {

    @Qualifier("fileProcessingScheduler")
    private final ScheduledExecutorService scheduler;

    private final ArchivoProcessingService processingService;

    /**
     * Demora en milisegundos entre la recepción del archivo y el inicio del análisis.
     * Configurable en {@code application.properties} con la clave
     * {@code file.processing.delay-ms}. Valor por defecto: 120 000 ms (2 minutos).
     */
    @Value("${file.processing.delay-ms:120000}")
    private long processingDelayMs;

    /**
     * Agenda el procesamiento de seguridad del archivo para ejecutarse tras
     * {@code file.processing.delay-ms} milisegundos.
     *
     * @param archivoId      ID del registro en BD (estado {@code PENDING})
     * @param quarantinePath ruta del archivo en cuarentena local
     */
    public void scheduleProcessing(Long archivoId, Path quarantinePath) {
        log.info("[SCHEDULER] Procesamiento agendado | archivoId={} delay={}ms path='{}'",
                archivoId, processingDelayMs, quarantinePath);

        scheduler.schedule(
                () -> {
                    log.info("[SCHEDULER] Disparando procesamiento | archivoId={}", archivoId);
                    processingService.procesarAsync(archivoId, quarantinePath);
                },
                processingDelayMs,
                TimeUnit.MILLISECONDS
        );
    }
}
