package br.com.konekta.ouvidoria.service;

import br.com.konekta.ouvidoria.model.enums.TipoManifestacao;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProtocoloService {
    
    private final AtomicLong sequence = new AtomicLong(0);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public String gerarProtocolo(TipoManifestacao tipo, Long numeroSequencial) {
        String data = LocalDateTime.now().format(dateFormatter);
        String prefixo = getPrefixoTipo(tipo);
        String sequencia = String.format("%04d", numeroSequencial);
        
        return prefixo + data + "-" + sequencia;
    }
    
    private String getPrefixoTipo(TipoManifestacao tipo) {
        switch (tipo) {
            case ELOGIO: return "ELG";
            case SUGESTAO: return "SUG";
            case RECLAMACAO: return "REC";
            case DENUNCIA: return "DEN";
            default: return "MAN";
        }
    }
}