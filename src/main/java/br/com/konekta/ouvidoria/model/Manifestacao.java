package br.com.konekta.ouvidoria.model;

import br.com.konekta.ouvidoria.model.enums.CategoriaDenuncia;
import br.com.konekta.ouvidoria.model.enums.TipoManifestacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "manifestacao")
public class Manifestacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoManifestacao tipo;

    @Enumerated(EnumType.STRING)
    private CategoriaDenuncia categoria;

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(length = 500)
    private String resumo;

    @Column(nullable = false)
    private String protocolo;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    // Construtores
    public Manifestacao() {}

    public Manifestacao(TipoManifestacao tipo, CategoriaDenuncia categoria, String descricao, 
                       String resumo, String protocolo, Usuario usuario) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.resumo = resumo;
        this.protocolo = protocolo;
        this.usuario = usuario;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoManifestacao getTipo() { return tipo; }
    public void setTipo(TipoManifestacao tipo) { this.tipo = tipo; }

    public CategoriaDenuncia getCategoria() { return categoria; }
    public void setCategoria(CategoriaDenuncia categoria) { this.categoria = categoria; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public String getProtocolo() { return protocolo; }
    public void setProtocolo(String protocolo) { this.protocolo = protocolo; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}