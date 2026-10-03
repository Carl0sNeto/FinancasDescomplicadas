package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    List<Meta> findByUsuarioIdOrderByDataLimiteAsc(UUID usuarioId);

    Optional<Meta> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Modifying
    @Query("delete from Meta m where m.usuario.id = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
