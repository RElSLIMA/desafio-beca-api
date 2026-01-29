package com.gabriel.desafio.beca.api.domain.repository;

import com.gabriel.desafio.beca.api.application.dto.AnaliseCategoriaDTO;
import com.gabriel.desafio.beca.api.application.dto.AnaliseDiariaDTO;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    @Query("SELECT t FROM Transacao t WHERE t.usuario.id = :usuarioId OR t.destinatario.id = :usuarioId ORDER BY t.data DESC")
    List<Transacao> findHistoricoCompleto(@Param("usuarioId") UUID usuarioId);

    @Query("""
        SELECT new com.gabriel.desafio.beca.api.application.dto.AnaliseDiariaDTO(
            CAST(t.data AS LocalDate),
            SUM(t.valor)
        )
        FROM Transacao t
        WHERE (t.usuario.id = :usuarioId OR t.destinatario.id = :usuarioId)
        AND t.data BETWEEN :inicio AND :fim
        GROUP BY CAST(t.data AS LocalDate)
        ORDER BY CAST(t.data AS LocalDate) ASC
    """)
    List<AnaliseDiariaDTO> agruparPorData(
            @Param("usuarioId") UUID usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );

    @Query("""
        SELECT new com.gabriel.desafio.beca.api.application.dto.AnaliseCategoriaDTO(
            t.categoria,
            SUM(t.valor)
        )
        FROM Transacao t
        WHERE (t.usuario.id = :usuarioId OR t.destinatario.id = :usuarioId)
        AND t.data BETWEEN :inicio AND :fim
        GROUP BY t.categoria
    """)
    List<AnaliseCategoriaDTO> agruparPorCategoria(
            @Param("usuarioId") UUID usuarioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}