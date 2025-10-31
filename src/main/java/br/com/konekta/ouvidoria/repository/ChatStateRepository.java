package br.com.konekta.ouvidoria.repository;

import br.com.konekta.ouvidoria.model.ChatState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatStateRepository extends JpaRepository<ChatState, String> {
    Optional<ChatState> findByPhoneNumber(String phoneNumber);
}