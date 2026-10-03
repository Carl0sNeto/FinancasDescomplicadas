package com.carlos.financasdescomplicadas.repository;

import com.carlos.financasdescomplicadas.dto.TotalPorCategoriaDTO;
import com.carlos.financasdescomplicadas.dto.TotaisPeriodoDTO;
import com.carlos.financasdescomplicadas.model.Transacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    @EntityGraph(attributePaths = "categoria")
    Optional<Transacao> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @EntityGraph(attributePaths = "categoria")
    List<Transacao> findByUsuarioIdAndDataBetweenOrderByDataDescCriadoEmDesc(
            UUID usuarioId, LocalDate inicio, LocalDate fim);

    @EntityGraph(attributePaths = "categoria")
    List<Transacao> findByUsuarioIdAndCategoriaIdAndDataBetweenOrderByDataDescCriadoEmDesc(
            UUID usuarioId, UUID categoriaId, LocalDate inicio, LocalDate fim);

    // Pendentes = sem categoria, independente do período
    List<Transacao> findByUsuarioIdAndCategoriaIsNullOrderByDataDesc(UUID usuarioId);

    long countByUsuarioIdAndCategoriaIsNull(UUID usuarioId);

    boolean existsByCategoriaId(UUID categoriaId);

    @Query("""
            select new com.carlos.financasdescomplicadas.dto.TotaisPeriodoDTO(
                coalesce(sum(case when t.valor > 0 then t.valor else 0 end), 0),
                coalesce(sum(case when t.valor < 0 then -t.valor else 0 end), 0))
            from Transacao t
            where t.usuario.id = :usuarioId and t.data between :inicio and :fim
            """)
    TotaisPeriodoDTO totaisDoPeriodo(@Param("usuarioId") UUID usuarioId,
                                     @Param("inicio") LocalDate inicio,
                                     @Param("fim") LocalDate fim);

    // Despesas agrupadas por categoria; categoria nula = pendentes de categorização
    @Query("""
            select new com.carlos.financasdescomplicadas.dto.TotalPorCategoriaDTO(
                c.id, c.nome, sum(-t.valor))
            from Transacao t left join t.categoria c
            where t.usuario.id = :usuarioId and t.data between :inicio and :fim and t.valor < 0
            group by c.id, c.nome
            order by sum(-t.valor) desc
            """)
    List<TotalPorCategoriaDTO> despesasPorCategoria(@Param("usuarioId") UUID usuarioId,
                                                    @Param("inicio") LocalDate inicio,
                                                    @Param("fim") LocalDate fim);

    @Modifying
    @Query("update Transacao t set t.categoria = null where t.categoria.id = :categoriaId")
    int descategorizarPorCategoria(@Param("categoriaId") UUID categoriaId);

    @Modifying
    @Query("delete from Transacao t where t.importacao.id = :importacaoId")
    int deleteByImportacaoId(@Param("importacaoId") UUID importacaoId);

    @Modifying
    @Query("delete from Transacao t where t.usuario.id = :usuarioId")
    int deleteByUsuarioId(@Param("usuarioId") UUID usuarioId);
}
