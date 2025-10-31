package br.com.konekta.ouvidoria.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GoogleCredentialsManager {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCredentialsManager.class);

    private final String credentialsJson;
    private GoogleCredentials cachedCredentials;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Object initLock = new Object();
    private long initializationTime;

    public GoogleCredentialsManager() {
        this.credentialsJson = loadCredentialsFromClasspath();
        logger.debug("GoogleCredentialsManager instantiated");

        // Validação imediata do JSON
        if (!isValidJson(credentialsJson)) {
            throw new IllegalStateException("credentials.json contém JSON malformado ou vazio");
        }
    }

    private String loadCredentialsFromClasspath() {
        try {
            Resource resource = new ClassPathResource("credentials.json");
            logger.info("📁 Loading credentials from: {}", resource.getURI());

            if (!resource.exists()) {
                throw new IllegalStateException("Arquivo credentials.json não encontrado no classpath");
            }

            String jsonContent;
            try (InputStream inputStream = resource.getInputStream()) {
                jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            logger.debug("Conteúdo do credentials.json carregado ({} bytes)", jsonContent.length());

            if (jsonContent.trim().isEmpty()) {
                throw new IllegalStateException("Arquivo credentials.json está vazio");
            }

            return jsonContent;

        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler credentials.json do classpath: " + e.getMessage(), e);
        }
    }

    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        String trimmed = json.trim();

        // Verifica se começa com { e termina com }
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            logger.error("❌ JSON inválido: não começa com '{' ou não termina com '}'");
            logger.error("Conteúdo: {}", json);
            return false;
        }

        // Verifica campos obrigatórios
        if (!json.contains("\"type\"") ||
                !json.contains("\"private_key\"") ||
                !json.contains("\"client_email\"")) {
            logger.error("❌ JSON inválido: campos obrigatórios faltando");
            return false;
        }

        return true;
    }

    public GoogleCredentials getCredentials() throws GoogleCredentialsException {
        if (!initialized.get()) {
            initializeCredentials();
        }
        return cachedCredentials;
    }

    public GoogleCredentials getCredentials(Set<String> scopes) throws GoogleCredentialsException {
        logger.debug("Getting credentials with scopes: {}", scopes);
        GoogleCredentials credentials = getCredentials();

        if (scopes != null && !scopes.isEmpty()) {
            return credentials.createScoped(scopes);
        }
        return credentials;
    }

    private void initializeCredentials() throws GoogleCredentialsException {
        if (initialized.get()) {
            return;
        }

        synchronized (initLock) {
            if (initialized.get()) {
                return;
            }

            logger.info("🔄 Initializing Google credentials...");
            long startTime = System.currentTimeMillis();

            try (InputStream credentialsStream = new ByteArrayInputStream(
                    credentialsJson.getBytes(StandardCharsets.UTF_8))) {

                logger.debug("Tentando fazer parse do JSON das credenciais...");
                this.cachedCredentials = GoogleCredentials.fromStream(credentialsStream);
                this.initialized.set(true);
                this.initializationTime = System.currentTimeMillis() - startTime;

                logger.info("✅ Google credentials initialized in {} ms", initializationTime);

            } catch (IOException e) {
                String errorMsg = "Failed to initialize Google credentials: " + e.getMessage();
                logger.error("❌ " + errorMsg);
                logger.error("Conteúdo do JSON que falhou: {}", credentialsJson);
                throw new GoogleCredentialsException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Unexpected error during credentials initialization: " + e.getMessage();
                logger.error("❌ " + errorMsg);
                throw new GoogleCredentialsException(errorMsg, e);
            }
        }
    }

    public boolean validateCredentials() {
        return isValidJson(credentialsJson);
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public long getInitializationTime() {
        return initializationTime;
    }

    public String getCredentialsInfo() {
        if (!isInitialized()) {
            return "Not initialized";
        }

        try {
            GoogleCredentials credentials = getCredentials();
            if (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
                var serviceAccount = (com.google.auth.oauth2.ServiceAccountCredentials) credentials;
                return String.format("ServiceAccount: %s", serviceAccount.getClientEmail());
            }
            return credentials.getClass().getSimpleName();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Método para debug - não usar em produção
    public String getCredentialsJsonPreview() {
        if (credentialsJson == null) return "null";
        if (credentialsJson.length() <= 100) return credentialsJson;
        return credentialsJson.substring(0, 100) + "...";
    }
}

class GoogleCredentialsException extends Exception {
    public GoogleCredentialsException(String message) {
        super(message);
    }

    public GoogleCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}