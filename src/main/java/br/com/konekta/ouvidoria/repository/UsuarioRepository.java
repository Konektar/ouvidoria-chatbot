package br.com.konekta.ouvidoria.repository;

import br.com.konekta.ouvidoria.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findAllByTelefone(String telefone);
    Optional<Usuario> findByTelefone(String telefone);
    boolean existsByTelefone(String telefone);
}