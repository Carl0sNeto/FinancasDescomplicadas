package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.model.RegraCategorizacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegraCategorizacaoRepository extends JpaRepository<RegraCategorizacao, UUID> {

    @EntityGraph(attributePaths = "categoria")
    List<RegraCategorizacao> findByUsuarioIdOrderByPalavraChaveAsc(UUID usuarioId);

    Optional<RegraCategorizacao> findByUsuarioIdAndPalavraChave(UUID usuarioId, String palavraChave);

    Optional<RegraCategorizacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Modifying
    @Query("delete from RegraCategorizacao r where r.categoria.id = :categoriaId")
    void deleteByCategoriaId(@Param("categoriaId") UUID categoriaId);

    @Modifying
    @Query("delete from RegraCategorizacao r where r.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
