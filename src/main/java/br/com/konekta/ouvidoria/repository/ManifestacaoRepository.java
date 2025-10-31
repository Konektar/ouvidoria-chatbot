package br.com.konekta.ouvidoria.repository;

import br.com.konekta.ouvidoria.model.Manifestacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ManifestacaoRepository extends JpaRepository<Manifestacao, Long> {
    List<Manifestacao> findByUsuarioTelefoneOrderByDataCriacaoDesc(String telefone);
    Long countByUsuarioTelefone(String telefone);
}