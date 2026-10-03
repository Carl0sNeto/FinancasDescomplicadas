package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.model.Meta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

    List<Meta> findByUsuarioIdOrderByDataLimiteAsc(UUID usuarioId);

    Optional<Meta> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
