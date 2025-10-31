package br.com.konekta.ouvidoria.model;

import br.com.konekta.ouvidoria.model.enums.LgpdConsentimento;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String nome;

    @Column(nullable = true)
    private String telefone;

    @Email
    private String email;

    @Column(nullable = false)
    private Boolean anonimo = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LgpdConsentimento lgpdConsentimento;

    @Column(nullable = false)
    private LocalDateTime dataConsentimento;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Manifestacao> manifestacoes = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataCriacao;


    // Construtores
    public Usuario() {}

    public Usuario(String nome, String telefone, String email, Boolean anonimo, LgpdConsentimento lgpdConsentimento) {
        this.nome = nome;
        this.telefone = telefone;
        this.email = email;
        this.anonimo = anonimo;
        this.lgpdConsentimento = lgpdConsentimento;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getAnonimo() { return anonimo; }
    public void setAnonimo(Boolean anonimo) { this.anonimo = anonimo; }

    public LgpdConsentimento getLgpdConsentimento() { return lgpdConsentimento; }
    public void setLgpdConsentimento(LgpdConsentimento lgpdConsentimento) { this.lgpdConsentimento = lgpdConsentimento; }

    public LocalDateTime getDataConsentimento() { return dataConsentimento; }
    public void setDataConsentimento(LocalDateTime dataConsentimento) { this.dataConsentimento = dataConsentimento; }

    public List<Manifestacao> getManifestacoes() { return manifestacoes; }
    public void setManifestacoes(List<Manifestacao> manifestacoes) { this.manifestacoes = manifestacoes; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    @PrePersist
    @PreUpdate
    protected void validate() {
        // Validação para usuários anônimos
        if (Boolean.TRUE.equals(anonimo)) {
            this.nome = null;
            this.telefone = null;
            this.email = null;
        } else {
            // Validação para usuários não anônimos - pelo menos um contato deve ser informado
            if ((nome == null || nome.trim().isEmpty()) &&
                    (telefone == null || telefone.trim().isEmpty()) &&
                    (email == null || email.trim().isEmpty())) {
                throw new IllegalStateException("Para usuários não anônimos, é necessário informar pelo menos um meio de contato (nome, telefone ou email)");
            }
        }

        // Validação de consentimento LGPD
        if (lgpdConsentimento == null) {
            throw new IllegalStateException("Consentimento LGPD é obrigatório");
        }

        // Se o consentimento for positivo, deve ter data de consentimento
        if (lgpdConsentimento == LgpdConsentimento.CONCORDO && dataConsentimento == null) {
            dataConsentimento = LocalDateTime.now();
        }

        // Validação básica de email se preenchido
        if (email != null && !email.trim().isEmpty()) {
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new IllegalStateException("Formato de email inválido");
            }
        }

        // Validação de telefone se preenchido (formato básico)
        if (telefone != null && !telefone.trim().isEmpty()) {
            String telefoneLimpo = telefone.replaceAll("\\D", "");
            if (telefoneLimpo.length() < 10 || telefoneLimpo.length() > 11) {
                throw new IllegalStateException("Telefone deve ter entre 10 e 11 dígitos");
            }
        }
        if(dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
        if (dataConsentimento == null && lgpdConsentimento == LgpdConsentimento.CONCORDO) {
            dataConsentimento = LocalDateTime.now();
        }
    }


}