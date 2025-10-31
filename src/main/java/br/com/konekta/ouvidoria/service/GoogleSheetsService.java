package br.com.konekta.ouvidoria.service;

import br.com.konekta.ouvidoria.config.GoogleCredentialsManager;
import br.com.konekta.ouvidoria.model.Manifestacao;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class GoogleSheetsService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);

    private final GoogleCredentialsManager credentialsManager;
    private Sheets sheetsService;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final Object initLock = new Object();

    private static final Set<String> SCOPES = Set.of(SheetsScopes.SPREADSHEETS);

    @Value("${app.sanity-check.enabled:true}")
    private boolean sanityCheckEnabled;

    @Value("${app.sheets.manifestacoes.spreadsheet-id:}")
    private String manifestacoesSpreadsheetId;

    @Value("${app.sheets.manifestacoes.sheet-name:Manifestacoes}")
    private String manifestacoesSheetName;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private long initializationTime;

    public GoogleSheetsService(GoogleCredentialsManager credentialsManager) {
        this.credentialsManager = credentialsManager;
        logger.info("GoogleSheetsService created - ready for initialization");
    }

    /**
     * Inicialização rápida do serviço Sheets
     */
    private void initializeSheetsService() throws GoogleSheetsException {
        if (initialized.get()) {
            return;
        }

        synchronized (initLock) {
            if (initialized.get()) {
                return;
            }

            logger.info("🔄 Initializing Google Sheets service...");
            long startTime = System.currentTimeMillis();

            try {
                GoogleCredentials credentials = credentialsManager.getCredentials(SCOPES);

                // Configuração correta do Sheets service
                final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                this.sheetsService = new Sheets.Builder(
                        httpTransport,
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials)
                ).setApplicationName("Bideo-Browser-Ouvidoria")
                        .build();

                this.initialized.set(true);
                this.initializationTime = System.currentTimeMillis() - startTime;

                logger.info("✅ Google Sheets service initialized in {} ms", initializationTime);

            } catch (GeneralSecurityException | IOException e) {
                String errorMsg = "Failed to initialize Sheets service: " + e.getMessage();
                logger.error("❌ " + errorMsg, e);
                throw new GoogleSheetsException(errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "Unexpected error during Sheets service initialization: " + e.getMessage();
                logger.error("❌ " + errorMsg, e);
                throw new GoogleSheetsException(errorMsg, e);
            }
        }
    }

    private Sheets getSheetsService() throws GoogleSheetsException {
        if (!initialized.get()) {
            initializeSheetsService();
        }
        return sheetsService;
    }

    /**
     * Adiciona uma nova manifestação ao Google Sheets
     */
    public void appendManifestacao(Manifestacao manifestacao, long numeroManifestacao) throws GoogleSheetsException {
        if (manifestacoesSpreadsheetId == null || manifestacoesSpreadsheetId.trim().isEmpty()) {
            logger.warn("⚠️ Manifestações spreadsheet ID not configured - skipping Google Sheets save");
            return;
        }

        try {
            logger.info("📝 Saving manifestação {} to Google Sheets...", numeroManifestacao);

            // Inicializa cabeçalhos se necessário
            initializeManifestacoesSheet();

            // Extrai dados da manifestação usando reflexão
            Map<String, Object> manifestacaoData = extractManifestacaoData(manifestacao, numeroManifestacao);

            // DEBUG: Log dos dados extraídos
            logger.debug("📊 Dados extraídos da manifestação: {}", manifestacaoData);

            // Prepara os dados para a planilha - CONVERTENDO TUDO PARA STRING
            List<Object> rowData = Arrays.asList(
                    manifestacaoData.get("protocolo"),
                    manifestacaoData.get("dataCriacao"),
                    manifestacaoData.get("tipo"),
                    manifestacaoData.get("categoria"),
                    manifestacaoData.get("descricao"),
                    manifestacaoData.get("resumo"),
                    manifestacaoData.get("usuario"),
                    manifestacaoData.get("telefone"),
                    manifestacaoData.get("email"),
                    manifestacaoData.get("anonimo"),
                    manifestacaoData.get("lgpd"),
                    manifestacaoData.get("dataConsentimento"),
                    "PENDENTE" // Status padrão
            );

            // DEBUG: Log da linha que será enviada
            logger.debug("📤 Linha para Google Sheets: {}", rowData);

            List<List<Object>> values = Arrays.asList(rowData);

            // Adiciona à planilha
            appendToSpreadsheet(manifestacoesSpreadsheetId, manifestacoesSheetName + "!A:I", values);

            logger.info("✅ Manifestação {} saved successfully to Google Sheets", numeroManifestacao);

        } catch (Exception e) {
            String errorMsg = "Failed to save manifestação to Google Sheets: " + e.getMessage();
            logger.error("❌ " + errorMsg, e);
            throw new GoogleSheetsException(errorMsg, e);
        }
    }

    /**
     * Escreve log de inicialização na mesma estrutura das manifestações
     */
    public void appendInicializacaoLog(String applicationName, String springProfile) throws GoogleSheetsException {
        if (manifestacoesSpreadsheetId == null || manifestacoesSpreadsheetId.trim().isEmpty()) {
            logger.warn("⚠️ Manifestações spreadsheet ID not configured - skipping initialization log");
            return;
        }

        try {
            logger.info("📝 Writing application initialization log to Google Sheets...");

            // Inicializa cabeçalhos se necessário
            initializeManifestacoesSheet();

            // Prepara os dados do log de inicialização
            String timestamp = LocalDateTime.now().format(formatter);
            String systemInfo = String.format("Spring Boot 3.2 | Profile: %s | Java %s | Memory: %dMB",
                    springProfile,
                    System.getProperty("java.version"),
                    Runtime.getRuntime().maxMemory() / (1024 * 1024));

            String initTimes = String.format("Creds: %dms, Sheets: %dms",
                    credentialsManager.getInitializationTime(),
                    initializationTime);

            // Usa a mesma estrutura das manifestações, mas com dados de log
            List<Object> rowData = Arrays.asList(
                    "LOG", // Número (especial para logs)
                    "APP_START", // Protocolo especial para inicialização
                    timestamp, // Data Criação
                    "SISTEMA", // Tipo
                    "INICIALIZACAO", // Categoria
                    "Aplicação inicializada com sucesso", // Descrição
                    systemInfo + " | " + initTimes, // Resumo (usa a coluna para info do sistema)
                    "SISTEMA", // Usuário
                    "✅ CONCLUÍDO" // Status
            );

            List<List<Object>> values = Arrays.asList(rowData);

            // Adiciona à planilha
            appendToSpreadsheet(manifestacoesSpreadsheetId, manifestacoesSheetName + "!A:I", values);

            logger.info("✅ Application initialization log saved successfully to Google Sheets");

        } catch (Exception e) {
            String errorMsg = "Failed to save initialization log to Google Sheets: " + e.getMessage();
            logger.error("❌ " + errorMsg, e);
            throw new GoogleSheetsException(errorMsg, e);
        }
    }

    /**
     * Extrai dados da manifestação usando reflexão e converte todos os valores para String
     */
    private Map<String, Object> extractManifestacaoData(Manifestacao manifestacao, long numeroManifestacao) {
            Map<String, Object> data = new HashMap<>();
            Class<?> manifestacaoClass = manifestacao.getClass();
            data.put("numero", manifestacao.getId());
            data.put("protocolo", convertToString(manifestacao.getProtocolo()));
            data.put("dataCriacao", convertToString(null != manifestacao.getDataCriacao() ? manifestacao.getDataCriacao() : LocalDateTime.now().format(formatter)));
            data.put("tipo", convertToString(manifestacao.getTipo().name()));
            if(manifestacao.getCategoria() != null) {
                data.put("categoria", convertToString(manifestacao.getCategoria().toString()));
            }else{
                data.put("categoria", "-");
            }
            data.put("descricao", convertToString(manifestacao.getDescricao()));
            data.put("resumo", convertToString(manifestacao.getResumo()));
            if(manifestacao.getUsuario() != null && manifestacao.getUsuario().getNome() != null) {
                data.put("usuario", convertToString(manifestacao.getUsuario().getNome()));
            }else{
                data.put("usuario","-");
            }
            if(manifestacao.getUsuario() != null && manifestacao.getUsuario().getTelefone() != null) {
                data.put("telefone", convertToString(manifestacao.getUsuario().getTelefone()));
            }else{
                data.put("telefone","-");
            }
            data.put("telefone",convertToString(manifestacao.getUsuario().getTelefone()));
            data.put("email",convertToString(manifestacao.getUsuario().getEmail()));
            data.put("anonimo",convertToString(manifestacao.getUsuario().getAnonimo()));
            data.put("lgpd",convertToString(manifestacao.getUsuario().getLgpdConsentimento()));
            data.put("dataConsentimento",convertToString(manifestacao.getUsuario().getDataConsentimento()));
        return data;
    }

    /**
     * Converte qualquer objeto para String, tratando especialmente Enums
     */
    private String convertToString(Object value) {
        if (value == null) {
            return "";
        }

        // Se for um Enum, pega o nome do Enum
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        // Se for LocalDateTime, formata
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(formatter);
        }

        // Para outros objetos, usa toString()
        return value.toString();
    }

    /**
     * Inicializa a planilha de manifestações com cabeçalhos
     */
    private void initializeManifestacoesSheet() throws GoogleSheetsException {
        if (manifestacoesSpreadsheetId == null) return;

        try {
            List<List<Object>> existingData = readSpreadsheet(manifestacoesSpreadsheetId, manifestacoesSheetName + "!A1:I1");

            if (existingData.isEmpty()) {
                List<List<Object>> headers = Arrays.asList(
                        Arrays.asList("Número", "Protocolo", "Data Criação", "Tipo", "Categoria",
                                "Descrição", "Resumo", "Usuário", "Status")
                );
                writeToSpreadsheet(manifestacoesSpreadsheetId, manifestacoesSheetName + "!A1:I1", headers);
                logger.info("📋 Initialized manifestações sheet with headers");
            }
        } catch (GoogleSheetsException e) {
            logger.debug("Could not initialize manifestações sheet headers: {}", e.getMessage());
        }
    }

    /**
     * Sanity Check assíncrono - não bloqueia a inicialização
     */
    @Async
    public CompletableFuture<Boolean> performSanityCheck(String applicationName, String springProfile) {
        if (!sanityCheckEnabled || manifestacoesSpreadsheetId == null || manifestacoesSpreadsheetId.trim().isEmpty()) {
            logger.info("⏭️ Sanity check disabled - no spreadsheet configured");
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("📝 Performing async sanity check...");

                // Inicializa o serviço se necessário
                initializeSheetsService();

                // Escreve o log de inicialização na planilha de manifestações
                appendInicializacaoLog(applicationName, springProfile);

                logger.info("✅ Sanity check completed successfully");
                return true;

            } catch (Exception e) {
                logger.warn("⚠️ Sanity check failed (non-critical): {}", e.getMessage());
                return false;
            }
        });
    }

    public List<List<Object>> readSpreadsheet(String spreadsheetId, String range) throws GoogleSheetsException {
        try {
            Sheets service = getSheetsService();
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> values = response.getValues();
            return values != null ? values : Collections.emptyList();

        } catch (IOException e) {
            throw new GoogleSheetsException("Failed to read spreadsheet: " + e.getMessage(), e);
        }
    }

    public void writeToSpreadsheet(String spreadsheetId, String range, List<List<Object>> values)
            throws GoogleSheetsException {
        try {
            Sheets service = getSheetsService();
            ValueRange body = new ValueRange().setValues(values);

            service.spreadsheets().values()
                    .update(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();

            logger.debug("📝 Wrote {} rows to {}", values.size(), spreadsheetId);

        } catch (IOException e) {
            throw new GoogleSheetsException("Failed to write to spreadsheet: " + e.getMessage(), e);
        }
    }

    public void appendToSpreadsheet(String spreadsheetId, String range, List<List<Object>> values)
            throws GoogleSheetsException {
        try {
            Sheets service = getSheetsService();
            ValueRange body = new ValueRange().setValues(values);

            service.spreadsheets().values()
                    .append(spreadsheetId, range, body)
                    .setValueInputOption("RAW")
                    .execute();

        } catch (IOException e) {
            throw new GoogleSheetsException("Failed to append to spreadsheet: " + e.getMessage(), e);
        }
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean healthCheck() {
        try {
            if (!isInitialized()) {
                initializeSheetsService();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Getters
    public boolean isSanityCheckEnabled() { return sanityCheckEnabled; }
    public String getManifestacoesSpreadsheetId() { return manifestacoesSpreadsheetId; }
    public String getManifestacoesSheetName() { return manifestacoesSheetName; }
    public long getInitializationTime() { return initializationTime; }
}

class GoogleSheetsException extends Exception {
    public GoogleSheetsException(String message) {
        super(message);
    }
    public GoogleSheetsException(String message, Throwable cause) {
        super(message, cause);
    }
}