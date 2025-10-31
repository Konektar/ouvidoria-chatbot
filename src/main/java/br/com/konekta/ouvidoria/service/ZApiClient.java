package br.com.konekta.ouvidoria.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;

@Service
public class ZApiClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ZApiClient.class);
    
    private final OkHttpClient client;
    private final String apiToken;
    private final String baseUrl;
    private final String instanceId;
    private final String clientId;
    
    public ZApiClient(@Value("${zapi.api.token}") String apiToken,
                     @Value("${zapi.base.url}") String baseUrl,
                     @Value("${zapi.instance.id}") String instanceId, @Value("${zapi.client.token}")String clientId) {
        this.client = new OkHttpClient();
        this.apiToken = apiToken;
        this.baseUrl = baseUrl;
        this.instanceId = instanceId;
        this.clientId = clientId;
    }
    
    public void sendTextMessage(String to, String message) throws IOException {
        String url = baseUrl + "/instances/" + instanceId + "/token/" + apiToken + "/send-text";
        
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("phone", to);
        jsonBody.put("message", message);
        
        RequestBody body = RequestBody.create(
            jsonBody.toString(), 
            MediaType.parse("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Token", clientId)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Erro ao enviar mensagem para {}: {}", to, response.body().string());
                throw new IOException("Erro na requisição: " + response.code());
            }
            logger.info("Mensagem enviada com sucesso para: {}", to);
        }
    }

    public void sendInteractiveMessage(String to, String title, String[] options) throws IOException {
        String url = baseUrl + "/instances/" + instanceId + "/token/" + apiToken + "/send-button-actions";

        logger.info("Enviando mensagem com botões de ação para: {}", to);
        logger.info("Título: {}", title);
        logger.info("Opções: {}", Arrays.toString(options));

        // Criar o payload conforme documentação do ZAPI
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("phone", to);
        jsonBody.put("message", title);
        jsonBody.put("title", "Ouvidoria - Escolha uma opção");
        jsonBody.put("footer", "Selecione uma das opções abaixo:");

        // Criar array de botões do tipo REPLY
        JSONArray buttonActions = new JSONArray();
        for (int i = 0; i < options.length; i++) {
            JSONObject button = new JSONObject();
            button.put("id",String.valueOf(i + 1));
            button.put("type", "REPLY");
            button.put("label", options[i]);
            buttonActions.put(button);
        }

        jsonBody.put("buttonActions", buttonActions);

        String jsonString = jsonBody.toString();
        logger.info("Payload JSON para send-button-actions: {}", jsonString);

        RequestBody body = RequestBody.create(
                jsonString,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Client-Token", clientId)
                .build();

        logger.info("Enviando requisição para: {}", url);

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            logger.info("Resposta do ZAPI - Status: {}, Body: {}", response.code(), responseBody);

            if (!response.isSuccessful()) {
                logger.error("Erro ao enviar mensagem com botões para {}: {}", to, responseBody);
                throw new IOException("Erro na requisição: " + response.code() + " - " + responseBody);
            } else {
                logger.info("Mensagem com botões enviada com sucesso para: {}", to);

                // Parse da resposta para obter IDs
                try {
                    JSONObject responseJson = new JSONObject(responseBody);
                    String messageId = responseJson.optString("messageId");
                    String zaapId = responseJson.optString("zaapId");
                    logger.info("MessageId: {}, ZaapId: {}", messageId, zaapId);
                } catch (Exception e) {
                    logger.warn("Não foi possível parsear a resposta do ZAPI: {}", responseBody);
                }
            }
        }
    }
}