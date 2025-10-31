package br.com.konekta.ouvidoria.model;

import br.com.konekta.ouvidoria.model.enums.EstadoChat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "chat_state")
public class ChatState {

    @Id
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoChat currentState;

    @ElementCollection
    @CollectionTable(name = "chat_state_context", joinColumns = @JoinColumn(name = "phone_number"))
    @MapKeyColumn(name = "context_key")
    @Column(name = "context_value", length = 1000)
    private Map<String, String> context = new HashMap<>();

    @Column(nullable = false)
    private LocalDateTime lastUpdate;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdate = LocalDateTime.now();
    }

    // Construtores
    public ChatState() {}

    public ChatState(String phoneNumber, EstadoChat currentState) {
        this.phoneNumber = phoneNumber;
        this.currentState = currentState;
    }

    // Getters e Setters
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public EstadoChat getCurrentState() { return currentState; }
    public void setCurrentState(EstadoChat currentState) { this.currentState = currentState; }

    public Map<String, String> getContext() { return context; }
    public void setContext(Map<String, String> context) { this.context = context; }

    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(LocalDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
}