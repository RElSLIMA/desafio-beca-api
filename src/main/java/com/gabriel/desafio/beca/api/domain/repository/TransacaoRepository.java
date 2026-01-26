package com.gabriel.desafio.beca.api.domain.repository;

import com.gabriel.desafio.beca.api.domain.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    @Query("SELECT t FROM Transacao t WHERE t.usuario.id = :usuarioId OR t.destinatario.id = :usuarioId ORDER BY t.data DESC")
    List<Transacao> findHistoricoCompleto(@Param("usuarioId") UUID usuarioId);
}