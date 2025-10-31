package br.com.konekta.ouvidoria;

import br.com.konekta.ouvidoria.service.GoogleSheetsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@EnableAsync
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final GoogleSheetsService sheetsService;
    private final Environment environment;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final long startupTime = System.currentTimeMillis();

    public Application(GoogleSheetsService sheetsService, Environment environment) {
        this.sheetsService = sheetsService;
        this.environment = environment;
    }

    public static void main(String[] args) {
        logger.info("🚀 Starting Bideo Browser Ouvidoria Application...");

        // Configurações para inicialização mais rápida
        System.setProperty("spring.main.lazy-initialization", "true");
        System.setProperty("spring.main.log-startup-info", "false");

        try {
            SpringApplication app = new SpringApplication(Application.class);

            // Inicialização mínima para startup rápido
            app.setLazyInitialization(true);
            app.setLogStartupInfo(false);

            var ctx = app.run(args);

            // Log rápido de inicialização
            long duration = System.currentTimeMillis() - ctx.getStartupDate();
            logger.info("✅ Application context loaded in {} ms", duration);

        } catch (Exception e) {
            logger.error("❌ Failed to start application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        long readyTime = System.currentTimeMillis() - startupTime;
        logger.info("🎯 Application ready in {} ms - starting async initialization...", readyTime);

        // Inicialização assíncrona dos serviços
        startAsyncInitialization();
    }

    /**
     * Inicialização assíncrona usando Thread tradicional
     */
    private void startAsyncInitialization() {
        new Thread(() -> {
            try {
                Thread.currentThread().setName("app-init-thread");

                // Pequena pausa para garantir que a aplicação já está respondendo
                Thread.sleep(1000);

                performFastStartupSequence();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Initialization interrupted");
            } catch (Exception e) {
                logger.error("Async initialization failed: {}", e.getMessage(), e);
            }
        }, "app-init-thread").start();
    }

    /**
     * Sequência de inicialização rápida
     */
    private void performFastStartupSequence() {
        long startTime = System.currentTimeMillis();

        try {
            // Passo 1: Inicialização rápida do serviço
            logger.info("🔧 Initializing core services...");
            sheetsService.healthCheck(); // Force initialization

            // Passo 2: Sanity Check assíncrono
            String appName = "Bideo Browser Ouvidoria";
            String profile = getActiveProfile();

            sheetsService.performSanityCheck(appName, profile)
                    .thenAccept(success -> {
                        if (success) {
                            logger.info("✅ Async sanity check completed successfully");
                        } else {
                            logger.warn("⚠️ Async sanity check completed with warnings");
                        }
                        logFinalStartupInfo(startTime);
                    });

        } catch (Exception e) {
            logger.error("❌ Startup sequence failed: {}", e.getMessage());
            logFinalStartupInfo(startTime);
        }
    }

    /**
     * Log final de informações do sistema
     */
    private void logFinalStartupInfo(long startTime) {
        long totalTime = System.currentTimeMillis() - startTime;

        logger.info("=" .repeat(70));
        logger.info("🚀 BIDEO BROWSER OUVIDORIA - STARTUP COMPLETE");
        logger.info("⏰ Startup Time: {} ms", totalTime);
        logger.info("📅 Ready at: {}", LocalDateTime.now().format(formatter));
        logger.info("🌐 Profile: {}", getActiveProfile());
        logger.info("💻 Java: {} | Memory: {}MB",
                System.getProperty("java.version"),
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        logger.info("📊 Sheets Service: {}",
                sheetsService.isInitialized() ? "✅ READY" : "⚠️ INITIALIZING");
        logger.info("🔐 Credentials: {}",
                sheetsService.healthCheck() ? "✅ VALID" : "❌ INVALID");
        logger.info("📝 Log written to Google Sheets: {}",
                sheetsService.getManifestacoesSpreadsheetId() != null ? "✅ YES" : "❌ NO");
        logger.info("=" .repeat(70));
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(", ", profiles) : "default";
    }
}