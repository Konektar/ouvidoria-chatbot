package br.com.konekta.ouvidoria.service;

import br.com.konekta.ouvidoria.model.ChatState;
import br.com.konekta.ouvidoria.model.Manifestacao;
import br.com.konekta.ouvidoria.model.Usuario;
import br.com.konekta.ouvidoria.model.enums.*;
import br.com.konekta.ouvidoria.repository.ChatStateRepository;
import br.com.konekta.ouvidoria.repository.ManifestacaoRepository;
import br.com.konekta.ouvidoria.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);

    private final ZApiClient zApiClient;
    private final GoogleSheetsService googleSheetsService;
    private final UsuarioRepository usuarioRepository;
    private final ManifestacaoRepository manifestacaoRepository;
    private final ChatStateRepository chatStateRepository;
    private final ProtocoloService protocoloService;

    private final Map<String, Map<String, Object>> sessionContext = new ConcurrentHashMap<>();

    public ChatbotService(@Lazy ZApiClient zApiClient,
                          GoogleSheetsService googleSheetsService,
                          UsuarioRepository usuarioRepository,
                          ManifestacaoRepository manifestacaoRepository,
                          ChatStateRepository chatStateRepository,
                          ProtocoloService protocoloService) {
        this.zApiClient = zApiClient;
        this.googleSheetsService = googleSheetsService;
        this.usuarioRepository = usuarioRepository;
        this.manifestacaoRepository = manifestacaoRepository;
        this.chatStateRepository = chatStateRepository;
        this.protocoloService = protocoloService;
        logger.info("ChatbotService inicializado com sucesso!");
    }

    @Transactional
    public void handleIncomingMessage(String from, String message) {
        try {
            // Normalizar o número de telefone
            String normalizedPhone = normalizePhoneNumber(from);

            ChatState chatState = getOrCreateChatState(normalizedPhone);
            EstadoChat currentState = chatState.getCurrentState();

            logger.info("Processando mensagem de {}: {} (Estado: {})", normalizedPhone, message, currentState);

            // Processar de acordo com o estado atual
            switch (currentState) {
                case INICIO:
                    handleInicio(normalizedPhone, message, chatState);
                    break;
                case IDENTIFICACAO:
                    handleIdentificacao(normalizedPhone, message, chatState);
                    break;
                case COLETA_IDENTIFICACAO:
                    handleColetaIdentificacao(normalizedPhone, message, chatState);
                    break;
                case LGPD:
                    handleLgpd(normalizedPhone, message, chatState);
                    break;
                case TIPO_MANIFESTACAO:
                    handleTipoManifestacao(normalizedPhone, message, chatState);
                    break;
                case CATEGORIA_DENUNCIA:
                    handleCategoriaDenuncia(normalizedPhone, message, chatState);
                    break;
                case COLETA_DETALHES:
                    handleColetaDetalhes(normalizedPhone, message, chatState);
                    break;
                case RESUMO_CONFIRMACAO:
                    handleResumoConfirmacao(normalizedPhone, message, chatState);
                    break;
                default:
                    resetToInicio(normalizedPhone, chatState);
                    break;
            }

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de " + from, e);
            sendMessage(from, "❌ Ocorreu um erro interno. Por favor, tente novamente.");
            resetChatState(from);
        }
    }

    // ========== MÉTODOS DE HANDLE POR ESTADO ==========

    private void handleInicio(String from, String message, ChatState chatState) throws IOException {
        sendInteractiveMessage(from,
                "🏢 *Bem-vindo à Ouvidoria!*\n\n" +
                        "Para começarmos, você deseja se identificar?",
                new String[]{"Sim", "Anonimato"}
        );
        updateChatState(chatState, EstadoChat.IDENTIFICACAO);
    }

    private void handleIdentificacao(String from, String message, ChatState chatState) throws IOException {
        if ("sim".equalsIgnoreCase(message)) {
            sendMessage(from, "📝 Por favor, informe seus dados no formato:\n*Nome, Telefone, Email*\n\nExemplo: João Silva, 11999999999, joao@email.com");
            updateChatState(chatState, EstadoChat.COLETA_IDENTIFICACAO);
        } else if ("anonimato".equalsIgnoreCase(message)) {
            createAnonymousUser(from, chatState);
            sendLgpdMessage(from, chatState);
        } else {
            sendMessage(from, "❌ Por favor, responda com *Sim* ou *Anonimato*");
        }
    }

    private void handleColetaIdentificacao(String from, String message, ChatState chatState) throws IOException {
        String[] dados = message.split(",");
        if (dados.length < 2) {
            sendMessage(from, "❌ Formato inválido. Por favor, informe:\n*Nome, Telefone, Email*");
            return;
        }

        String nome = dados[0].trim();
        String telefone = dados[1].trim();
        String email = dados.length > 2 ? dados[2].trim() : null;

        // Validar telefone
        if (!telefone.matches("\\d{10,13}")) {
            sendMessage(from, "❌ Telefone inválido. Por favor, informe apenas números (com DDD).");
            return;
        }

        // Salvar contexto temporário
        Map<String, Object> context = getSessionContext(from);
        context.put("nome", nome);
        context.put("telefone", telefone);
        context.put("email", email);
        context.put("anonimo", false);

        sendLgpdMessage(from, chatState);
    }

    private void handleLgpd(String from, String message, ChatState chatState) throws IOException {
        if ("concordo".equalsIgnoreCase(message)) {
            Map<String, Object> context = getSessionContext(from);

            Usuario usuario;

            if (Boolean.TRUE.equals(context.get("anonimo"))) {
                usuario = createAnonymousUser(from, chatState);
            } else {
                usuario = createIdentifiedUser(
                        (String) context.get("nome"),
                        (String) context.get("telefone"),
                        (String) context.get("email")
                );
            }

            context.put("usuario", usuario);
            sendTipoManifestacaoMessage(from, chatState);

        } else if ("não concordo".equalsIgnoreCase(message) || "nao concordo".equalsIgnoreCase(message)) {
            sendMessage(from, "❌ Infelizmente não podemos prosseguir sem seu consentimento com a LGPD.\n\nObrigado pelo contato! 👋");
            resetChatState(from);
        } else {
            sendMessage(from, "❌ Por favor, responda com *Concordo* ou *Não concordo*");
        }
    }

    private void handleTipoManifestacao(String from, String message, ChatState chatState) throws IOException {
        try {
            TipoManifestacao tipo = TipoManifestacao.valueOf(message.toUpperCase());
            Map<String, Object> context = getSessionContext(from);
            context.put("tipoManifestacao", tipo);

            if (tipo == TipoManifestacao.DENUNCIA) {
                sendCategoriaDenunciaMessage(from, chatState);
            } else {
                sendColetaDetalhesMessage(from, chatState, tipo);
            }

        } catch (IllegalArgumentException e) {
            sendMessage(from, "❌ Tipo de manifestação inválido. Por favor, escolha uma das opções acima.");
        }
    }

    private void handleCategoriaDenuncia(String from, String message, ChatState chatState) throws IOException {
        try {
            CategoriaDenuncia categoria = CategoriaDenuncia.valueOf(message.toUpperCase());
            Map<String, Object> context = getSessionContext(from);
            context.put("categoriaDenuncia", categoria);

            sendColetaDetalhesMessage(from, chatState, TipoManifestacao.DENUNCIA);

        } catch (IllegalArgumentException e) {
            sendMessage(from, "❌ Categoria inválida. Por favor, escolha uma das opções acima.");
        }
    }

    private void handleColetaDetalhes(String from, String message, ChatState chatState) throws IOException {
        Map<String, Object> context = getSessionContext(from);
        context.put("descricao", message);

        // Gerar resumo automático
        String resumo = generateResumo(context);
        context.put("resumo", resumo);

        sendResumoConfirmacao(from, chatState, resumo);
    }

    private void handleResumoConfirmacao(String from, String message, ChatState chatState) throws IOException, GoogleSheetsException {
        if ("confirmar".equalsIgnoreCase(message)) {
            finalizarManifestacao(from, chatState);
        } else if ("corrigir".equalsIgnoreCase(message)) {
            Map<String, Object> context = getSessionContext(from);
            TipoManifestacao tipo = (TipoManifestacao) context.get("tipoManifestacao");
            sendColetaDetalhesMessage(from, chatState, tipo);
        } else {
            sendMessage(from, "❌ Por favor, responda com *Confirmar* ou *Corrigir*");
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String normalizePhoneNumber(String phone) {
        // Remove qualquer caractere não numérico
        String cleaned = phone.replaceAll("[^0-9]", "");

        // Se não começar com 55 (Brasil), adiciona
        if (!cleaned.startsWith("55")) {
            // Assume que é um número brasileiro sem código do país
            if (cleaned.length() <= 11) {
                cleaned = "55" + cleaned;
            }
        }

        return cleaned;
    }

    private ChatState getOrCreateChatState(String phoneNumber) {
        return chatStateRepository.findByPhoneNumber(phoneNumber)
                .orElse(new ChatState(phoneNumber, EstadoChat.INICIO));
    }

    @Transactional
    private void updateChatState(ChatState chatState, EstadoChat newState) {
        chatState.setCurrentState(newState);
        chatStateRepository.save(chatState);
    }

    @Transactional
    private void resetChatState(String phoneNumber) {
        chatStateRepository.findByPhoneNumber(phoneNumber).ifPresent(chatState -> {
            chatState.setCurrentState(EstadoChat.INICIO);
            chatState.getContext().clear();
            chatStateRepository.save(chatState);
        });
    }

    private Map<String, Object> getSessionContext(String phoneNumber) {
        return sessionContext.computeIfAbsent(phoneNumber, k -> new HashMap<>());
    }

    private Usuario createAnonymousUser(String from, ChatState chatState) {
        // *** MÉTODO CORRIGIDO AQUI ***
        // Para usuário anônimo, sempre criar novo usuário com dados limpos
        Usuario usuario = new Usuario();
        usuario.setNome(null);  // Nome deve ser nulo para anônimo
        usuario.setTelefone(from);
        usuario.setEmail(null);  // Email deve ser nulo para anônimo
        usuario.setAnonimo(true);
        usuario.setLgpdConsentimento(LgpdConsentimento.CONCORDO);
        usuario.setDataConsentimento(LocalDateTime.now());

        // Salvar no contexto
        Map<String, Object> context = getSessionContext(from);
        context.put("anonimo", true);
        context.put("usuario", usuario);

        return usuarioRepository.save(usuario);
    }

    private Usuario createIdentifiedUser(String nome, String telefone, String email) {
        // *** MÉTODO CORRIGIDO AQUI ***
        // Para usuário identificado, buscar por telefone e atualizar ou criar novo
        Optional<Usuario> existingUser = usuarioRepository.findByTelefone(telefone);

        if (existingUser.isPresent()) {
            Usuario usuario = existingUser.get();
            // Atualizar dados do usuário existente
            usuario.setNome(nome);
            usuario.setEmail(email);
            usuario.setAnonimo(false);  // Garantir que não seja anônimo
            usuario.setLgpdConsentimento(LgpdConsentimento.CONCORDO);
            usuario.setDataConsentimento(LocalDateTime.now());
            return usuarioRepository.save(usuario);
        } else {
            Usuario usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setTelefone(telefone);
            usuario.setEmail(email);
            usuario.setAnonimo(false);
            usuario.setLgpdConsentimento(LgpdConsentimento.CONCORDO);
            usuario.setDataConsentimento(LocalDateTime.now());
            return usuarioRepository.save(usuario);
        }
    }

    private String generateResumo(Map<String, Object> context) {
        TipoManifestacao tipo = (TipoManifestacao) context.get("tipoManifestacao");
        String descricao = (String) context.get("descricao");

        String resumo = descricao.length() > 100 ?
                descricao.substring(0, 100) + "..." : descricao;

        return "[" + tipo.toString() + "] " + resumo;
    }

    private void finalizarManifestacao(String from, ChatState chatState) throws GoogleSheetsException {
        Map<String, Object> context = getSessionContext(from);

        Usuario usuario = (Usuario) context.get("usuario");
        TipoManifestacao tipo = (TipoManifestacao) context.get("tipoManifestacao");
        CategoriaDenuncia categoria = (CategoriaDenuncia) context.get("categoriaDenuncia");
        String descricao = (String) context.get("descricao");
        String resumo = (String) context.get("resumo");

        // Contar manifestações do usuário
        Long totalManifestacoes = manifestacaoRepository.countByUsuarioTelefone(usuario.getTelefone());

        // Gerar protocolo
        String protocolo = protocoloService.gerarProtocolo(tipo, totalManifestacoes + 1);

        // Salvar manifestação
        Manifestacao manifestacao = new Manifestacao();
        manifestacao.setTipo(tipo);
        //TODO arrumar a categoria da denuncia depois
        manifestacao.setCategoria(categoria);
        manifestacao.setDescricao(descricao);
        manifestacao.setResumo(resumo);
        manifestacao.setProtocolo(protocolo);
        manifestacao.setUsuario(usuario);

        manifestacaoRepository.save(manifestacao);

        // Salvar no Google Sheets
        googleSheetsService.appendManifestacao(manifestacao, totalManifestacoes + 1);

        // Enviar protocolo
        sendMessage(from,
                "✅ *Manifestação registrada com sucesso!*\n\n" +
                        "📋 *Protocolo:* " + protocolo + "\n" +
                        "📝 *Tipo:* " + tipo.toString() + "\n" +
                        "⏰ *Data:* " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "\n\n" +
                        "Agradecemos seu contato! 👋"
        );

        resetChatState(from);
    }

    private void resetToInicio(String from, ChatState chatState) throws IOException {
        resetChatState(from);
        handleInicio(from, "", chatState);
    }

    // ========== MÉTODOS DE ENVIO DE MENSAGENS ==========

    private void sendMessage(String to, String message) {
        try {
            zApiClient.sendTextMessage(to, message);
        } catch (IOException e) {
            logger.error("Erro ao enviar mensagem para " + to, e);
        }
    }

    private void sendInteractiveMessage(String to, String title, String[] options) throws IOException {
        zApiClient.sendInteractiveMessage(to, title, options);
    }

    private void sendLgpdMessage(String from, ChatState chatState) throws IOException {
        sendInteractiveMessage(from,
                "🔒 *Termos de Consentimento LGPD*\n\n" +
                        "Para continuarmos, precisamos do seu consentimento para o tratamento de dados pessoais conforme a Lei Geral de Proteção de Dados.",
                new String[]{"Concordo", "Não concordo"}
        );
        updateChatState(chatState, EstadoChat.LGPD);
    }

    private void sendTipoManifestacaoMessage(String from, ChatState chatState) throws IOException {
        sendInteractiveMessage(from,
                "📋 *Tipo de Manifestação*\n\n" +
                        "Por favor, selecione o tipo da sua manifestação:",
                Arrays.stream(TipoManifestacao.values())
                        .map(Enum::toString)
                        .toArray(String[]::new)
        );
        updateChatState(chatState, EstadoChat.TIPO_MANIFESTACAO);
    }

    private void sendCategoriaDenunciaMessage(String from, ChatState chatState) throws IOException {
        sendInteractiveMessage(from,
                "🚨 *Categoria da Denúncia*\n\n" +
                        "Por favor, selecione a categoria mais adequada:",
                Arrays.stream(CategoriaDenuncia.values())
                        .map(Enum::toString)
                        .toArray(String[]::new)
        );
        updateChatState(chatState, EstadoChat.CATEGORIA_DENUNCIA);
    }

    private void sendColetaDetalhesMessage(String from, ChatState chatState, TipoManifestacao tipo) throws IOException {
        String prompt = "";
        switch (tipo) {
            case ELOGIO:
                prompt = "🌟 *Elogio*\n\nPor favor, descreva seu elogio detalhadamente:";
                break;
            case SUGESTAO:
                prompt = "💡 *Sugestão*\n\nPor favor, descreva sua sugestão detalhadamente:";
                break;
            case RECLAMACAO:
                prompt = "⚠️ *Reclamação*\n\nPor favor, descreva sua reclamação detalhadamente:";
                break;
            case DENUNCIA:
                prompt = "🚨 *Denúncia*\n\nPor favor, descreva os fatos detalhadamente:";
                break;
        }

        sendMessage(from, prompt);
        updateChatState(chatState, EstadoChat.COLETA_DETALHES);
    }

    private void sendResumoConfirmacao(String from, ChatState chatState, String resumo) throws IOException {
        sendInteractiveMessage(from,
                "📄 *Resumo da Manifestação*\n\n" +
                        resumo + "\n\n" +
                        "Por favor, confirme se está tudo correto:",
                new String[]{"Confirmar", "Corrigir"}
        );
        updateChatState(chatState, EstadoChat.RESUMO_CONFIRMACAO);
    }
}