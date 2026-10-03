package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.model.Importacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportacaoRepository extends JpaRepository<Importacao, UUID> {

    List<Importacao> findByUsuarioIdOrderByImportadoEmDesc(UUID usuarioId);

    Optional<Importacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Modifying
    @Query("delete from Importacao i where i.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
