package br.com.konekta.ouvidoria.controller;

import br.com.konekta.ouvidoria.service.ChatbotService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final ChatbotService chatbotService;

    public WebhookController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("service", "Ouvidoria Chatbot");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ Teste OK - " + LocalDateTime.now());
    }

    @PostMapping("/zapi")
    public ResponseEntity<String> handleZapiWebhook(@RequestBody String payloadStr) {
        try {
            // Imprimir o payload string de forma bonita e formatada
            logger.debug("=== PAYLOAD STRING RECEBIDO ===");
            try {
                ObjectMapper prettyMapper = new ObjectMapper();
                Object jsonObject = prettyMapper.readValue(payloadStr, Object.class);
                String prettyPayload = prettyMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
                logger.debug("Payload formatado:\n{}", prettyPayload);
            } catch (Exception e) {
                logger.debug("Payload string (não foi possível formatar): {}", payloadStr);
            }
            logger.debug("=== FIM DO PAYLOAD STRING ===");

            // Converter a string em objeto ignorando campos desconhecidos
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            ZApiWebhookPayload payload = mapper.readValue(payloadStr, ZApiWebhookPayload.class);

            logger.info("Webhook convertido para objeto com sucesso");

            // Ignorar mensagens enviadas por nós mesmos ou status
            if (payload.isFromMe() || "MessageStatusCallback".equals(payload.getType())) {
                return ResponseEntity.ok("Mensagem ignorada");
            }

            String from = payload.getPhone();
            String text;

            // Verificar se é uma resposta de botão (agora no campo buttonReply)
            if (payload.getButtonReply() != null) {
                text = payload.getButtonReply().getMessage();
                logger.info("Resposta de botão detectada. ButtonId: {}, Mensagem: {}",
                        payload.getButtonReply().getButtonId(), text);
            }
            // Verificar se é mensagem de texto normal
            else if (payload.getText() != null && payload.getText().getMessage() != null) {
                text = payload.getText().getMessage();
            } else {
                text = null;
            }

            if (from != null && text != null && !text.trim().isEmpty()) {
                logger.info("Processando mensagem de {}: {}", from, text);

                new Thread(() -> {
                    try {
                        chatbotService.handleIncomingMessage(from, text.trim());
                    } catch (Exception e) {
                        logger.error("Erro ao processar mensagem", e);
                    }
                }).start();

                return ResponseEntity.ok("Mensagem recebida e sendo processada");
            } else {
                logger.warn("Mensagem não processada - from: {}, text: {}", from, text);
            }

            return ResponseEntity.ok("Tipo de mensagem não suportado");

        } catch (Exception e) {
            logger.error("Erro ao processar webhook do ZAPI", e);
            logger.error("Payload string que causou erro: {}", payloadStr);
            return ResponseEntity.internalServerError().body("Erro interno: " + e.getMessage());
        }
    }


    // DTOs atualizados baseados no JSON real do ZAPI
    public static class ZApiWebhookPayload {
        private boolean isStatusReply;
        private String senderLid; // Note: agora é senderLid, não chatLid
        private String connectedPhone;
        private boolean waitingMessage;
        private boolean isEdit;
        private boolean isGroup;
        private boolean isNewsletter;
        private String instanceId;
        private String messageId;
        private String phone;
        private boolean fromMe;
        private long momment;
        private String status;
        private String chatName;
        private String senderPhoto;
        private String senderName;
        private String participantPhone; // Novo campo
        private String participantLid;
        private String photo;
        private boolean broadcast;
        private String referenceMessageId; // Novo campo
        private boolean forwarded;
        private String type;
        private boolean fromApi;
        private TextObject text;

        public ButtonReply getButtonReply() {
            return buttonReply;
        }

        public void setButtonReply(ButtonReply buttonReply) {
            this.buttonReply = buttonReply;
        }

        private ButtonReply buttonReply; // NOVO CAMPO: buttonReply em vez de buttonsResponseMessage
        private TraceContext _traceContext;

        // Getters and Setters
        public boolean isStatusReply() { return isStatusReply; }
        public void setStatusReply(boolean statusReply) { isStatusReply = statusReply; }

        public String getSenderLid() { return senderLid; }
        public void setSenderLid(String senderLid) { this.senderLid = senderLid; }

        public String getConnectedPhone() { return connectedPhone; }
        public void setConnectedPhone(String connectedPhone) { this.connectedPhone = connectedPhone; }

        public boolean isWaitingMessage() { return waitingMessage; }
        public void setWaitingMessage(boolean waitingMessage) { this.waitingMessage = waitingMessage; }

        public boolean isEdit() { return isEdit; }
        public void setEdit(boolean edit) { isEdit = edit; }

        public boolean isGroup() { return isGroup; }
        public void setGroup(boolean group) { isGroup = group; }

        public boolean isNewsletter() { return isNewsletter; }
        public void setNewsletter(boolean newsletter) { isNewsletter = newsletter; }

        public String getInstanceId() { return instanceId; }
        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public boolean isFromMe() { return fromMe; }
        public void setFromMe(boolean fromMe) { this.fromMe = fromMe; }

        public long getMomment() { return momment; }
        public void setMomment(long momment) { this.momment = momment; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getChatName() { return chatName; }
        public void setChatName(String chatName) { this.chatName = chatName; }

        public String getSenderPhoto() { return senderPhoto; }
        public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }

        public String getParticipantPhone() { return participantPhone; }
        public void setParticipantPhone(String participantPhone) { this.participantPhone = participantPhone; }

        public String getParticipantLid() { return participantLid; }
        public void setParticipantLid(String participantLid) { this.participantLid = participantLid; }

        public String getPhoto() { return photo; }
        public void setPhoto(String photo) { this.photo = photo; }

        public boolean isBroadcast() { return broadcast; }
        public void setBroadcast(boolean broadcast) { this.broadcast = broadcast; }

        public String getReferenceMessageId() { return referenceMessageId; }
        public void setReferenceMessageId(String referenceMessageId) { this.referenceMessageId = referenceMessageId; }

        public boolean isForwarded() { return forwarded; }
        public void setForwarded(boolean forwarded) { this.forwarded = forwarded; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public boolean isFromApi() { return fromApi; }
        public void setFromApi(boolean fromApi) { this.fromApi = fromApi; }

        public TextObject getText() { return text; }
        public void setText(TextObject text) { this.text = text; }


        public TraceContext get_traceContext() { return _traceContext; }
        public void set_traceContext(TraceContext _traceContext) { this._traceContext = _traceContext; }
    }
    // NOVA CLASSE: ButtonReply para substituir ButtonsResponseMessage
    public static class ButtonReply {
        private String buttonId;
        private String message;
        private String referenceMessageId;

        public String getButtonId() { return buttonId; }
        public void setButtonId(String buttonId) { this.buttonId = buttonId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getReferenceMessageId() { return referenceMessageId; }
        public void setReferenceMessageId(String referenceMessageId) { this.referenceMessageId = referenceMessageId; }

        @Override
        public String toString() {
            return "ButtonReply{buttonId='" + buttonId + "', message='" + message + "', referenceMessageId='" + referenceMessageId + "'}";
        }
    }

    public static class TextObject {
        private String message;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // CLASSE CORRIGIDA: ButtonsResponseMessage
    public static class ButtonsResponseMessage {
        private String buttonId;
        private String message;

        public String getButtonId() { return buttonId; }
        public void setButtonId(String buttonId) { this.buttonId = buttonId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        @Override
        public String toString() {
            return "ButtonsResponseMessage{buttonId='" + buttonId + "', message='" + message + "'}";
        }
    }

    public static class TraceContext {
        private String traceId;
        private String spanId;

        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }

        public String getSpanId() { return spanId; }
        public void setSpanId(String spanId) { this.spanId = spanId; }
    }

}