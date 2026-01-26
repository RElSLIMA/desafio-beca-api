package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import com.gabriel.desafio.beca.api.domain.repository.TransacaoRepository;
import com.gabriel.desafio.beca.api.domain.repository.UsuarioRepository;
import com.gabriel.desafio.beca.api.application.dto.ExtratoDTO;
import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.application.dto.TransacaoDTO;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.messaging.TransacaoProducer;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransacaoService {

    @Autowired
    private TransacaoRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BrasilApiClient brasilApiClient;

    @Autowired
    private MockSaldoClient mockSaldoClient;

    @Autowired
    private TransacaoProducer transacaoProducer;

    @Transactional
    public Transacao registrar(TransacaoDTO dados) {
        Usuario usuario = usuarioRepository.findById(dados.usuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário remetente não encontrado!"));

        System.out.println("--- Recebendo solicitação de Transação para: " + usuario.getNome() + " ---");

        Usuario destinatario = null;
        if (dados.tipo() == TipoTransacao.TRANSFERENCIA) {
            if (dados.destinatarioId() == null) {
                throw new IllegalArgumentException("Para transferência, o ID do destinatário é obrigatório!");
            }
            if (dados.usuarioId().equals(dados.destinatarioId())) {
                throw new IllegalArgumentException("Não é possível transferir para a mesma conta!");
            }
            destinatario = usuarioRepository.findById(dados.destinatarioId())
                    .orElseThrow(() -> new EntityNotFoundException("Usuário destinatário não encontrado!"));
        }

        if (dados.tipo() == TipoTransacao.SAQUE || dados.tipo() == TipoTransacao.TRANSFERENCIA) {
            BigDecimal saldoAtual = mockSaldoClient.buscarSaldo(usuario.getId().toString());

            if (saldoAtual.compareTo(dados.valor()) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente. Saldo atual: " + saldoAtual);
            }
        }

        BigDecimal cotacaoAtual = BigDecimal.ZERO;
        try {
            var cambioDTO = brasilApiClient.buscarCotacaoDolar();
            if (cambioDTO != null) {
                cotacaoAtual = cambioDTO.valor();
            }
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível buscar cotação do dólar (Seguindo fluxo).");
        }

        Transacao novaTransacao = new Transacao(
                dados.valor(),
                dados.tipo(),
                usuario,
                destinatario,
                cotacaoAtual
        );
        novaTransacao.setStatus(StatusTransacao.PENDING);

        Transacao transacaoSalva = repository.save(novaTransacao);

        try {
            transacaoProducer.enviarEvento(transacaoSalva);
            System.out.println("API: Evento enviado para o Kafka. ID: " + transacaoSalva.getId());
        } catch (Exception e) {
            System.err.println("CRÍTICO: Erro ao enviar para o Kafka: " + e.getMessage());
        }

        return transacaoSalva;
    }

    public BigDecimal consultarSaldo(UUID usuarioId) {
        return mockSaldoClient.buscarSaldo(usuarioId.toString());
    }

    public ExtratoDTO buscarExtrato(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        BigDecimal saldo = mockSaldoClient.buscarSaldo(usuarioId.toString());

        List<Transacao> historico = repository.findHistoricoCompleto(usuarioId);

        return new ExtratoDTO(
                usuario.getNome(),
                saldo,
                historico
        );
    }
}