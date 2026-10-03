package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.model.Importacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacaoRepository extends JpaRepository<Importacao, UUID> {

    List<Importacao> findByUsuarioIdOrderByImportadoEmDesc(UUID usuarioId);

    Optional<Importacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
