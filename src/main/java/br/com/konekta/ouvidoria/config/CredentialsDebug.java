package br.com.konekta.ouvidoria.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class CredentialsDebug implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(CredentialsDebug.class);
    
    private final GoogleCredentialsManager credentialsManager;

    public CredentialsDebug(GoogleCredentialsManager credentialsManager) {
        this.credentialsManager = credentialsManager;
    }

    @Override
    public void run(String... args) throws Exception {
        debugCredentialsFile();
    }
    
    private void debugCredentialsFile() {
        try {
            Resource resource = new ClassPathResource("credentials.json");
            logger.info("=== DEBUG CREDENTIALS.JSON ===");
            logger.info("📁 File exists: {}", resource.exists());
            logger.info("📁 File URI: {}", resource.exists() ? resource.getURI() : "N/A");
            logger.info("📁 File description: {}", resource.getDescription());
            
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    logger.info("📁 File size: {} bytes", content.length());
                    logger.info("📁 File content preview: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
                    logger.info("📁 First 10 chars: '{}'", content.substring(0, Math.min(10, content.length())));
                    logger.info("📁 Is empty: {}", content.trim().isEmpty());
                    logger.info("📁 Starts with {{}: {}", content.trim().startsWith("{"));
                    logger.info("📁 Ends with }}: {}", content.trim().endsWith("}"));
                }
            }
            
            logger.info("📁 Credentials validation: {}", credentialsManager.validateCredentials());
            logger.info("📁 Credentials JSON preview: {}", credentialsManager.getCredentialsJsonPreview());
            logger.info("=== END DEBUG ===");
            
        } catch (IOException e) {
            logger.error("❌ Error during credentials debug: {}", e.getMessage());
        }
    }
}