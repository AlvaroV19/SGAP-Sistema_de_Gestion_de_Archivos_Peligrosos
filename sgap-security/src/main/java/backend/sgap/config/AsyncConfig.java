package backend.sgap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Configuración del subsistema asíncrono de SGAP.
 *
 * <ul>
 *   <li>Habilita {@code @Async} mediante {@link EnableAsync}.</li>
 *   <li>Habilita {@code @Scheduled} mediante {@link EnableScheduling} (para tareas periódicas futuras).</li>
 *   <li>Define el {@link Executor} que Spring usa para {@code @Async}.</li>
 *   <li>Define el {@link ScheduledExecutorService} para la demora configurable de procesamiento.</li>
 * </ul>
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Value("${file.processing.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${file.processing.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${file.processing.async.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Executor dedicado para el procesamiento asíncrono de archivos.
     * Nombrado "fileProcessingExecutor" para que {@code @Async} pueda
     * referenciarlo explícitamente y no interferir con otros executors.
     */
    @Bean(name = "fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("sgap-file-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * ScheduledExecutorService de un solo hilo para programar la demora de procesamiento.
     * Un hilo es suficiente: solo agenda tareas, no las ejecuta (las delega al pool anterior).
     */
    @Bean(name = "fileProcessingScheduler")
    public ScheduledExecutorService fileProcessingScheduler() {
        return Executors.newScheduledThreadPool(
                2,
                r -> {
                    Thread t = new Thread(r, "sgap-scheduler-" + System.nanoTime());
                    t.setDaemon(true);
                    return t;
                }
        );
    }
}
