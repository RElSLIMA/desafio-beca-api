package com.gabriel.desafio.beca.api.domain.transacao;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {
    List<Transacao> findByUsuarioId(UUID usuarioId);
}