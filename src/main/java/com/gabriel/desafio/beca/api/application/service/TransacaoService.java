package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.application.dto.AnaliseCategoriaDTO;
import com.gabriel.desafio.beca.api.application.dto.AnaliseDiariaDTO;
import com.gabriel.desafio.beca.api.application.dto.ExtratoDTO;
import com.gabriel.desafio.beca.api.application.dto.TransacaoDTO;
import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import com.gabriel.desafio.beca.api.domain.repository.TransacaoRepository;
import com.gabriel.desafio.beca.api.domain.repository.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.messaging.TransacaoProducer;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TransacaoService {

    private static final Logger log = LoggerFactory.getLogger(TransacaoService.class);

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
        log.info("Iniciando registro de transação ({}) - Tipo: {} - Categoria: {} - Usuário: {}",
                dados.moeda(), dados.tipo(), dados.categoria(), dados.usuarioId());

        Usuario usuario = usuarioRepository.findById(dados.usuarioId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Usuario destinatario = buscarDestinatario(dados);
        BigDecimal taxaCambio = obterTaxaCambio(dados.moeda());

        Transacao transacao = new Transacao(
                dados.valor(),
                dados.tipo(),
                dados.categoria(),
                usuario,
                destinatario,
                taxaCambio,
                dados.moeda()
        );
        transacao.setStatus(StatusTransacao.PENDING);

        repository.save(transacao);
        notificarKafka(transacao);

        return transacao;
    }

    public BigDecimal consultarSaldo(UUID usuarioId) {
        return mockSaldoClient.buscarSaldo(usuarioId.toString());
    }

    public ExtratoDTO buscarExtrato(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        BigDecimal saldoMock = mockSaldoClient.buscarSaldo(usuarioId.toString());
        List<Transacao> historico = repository.findHistoricoCompleto(usuarioId);

        return new ExtratoDTO(usuario.getNome(), saldoMock, historico);
    }

    @Transactional(readOnly = true)
    public List<AnaliseDiariaDTO> analisarPeriodo(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        log.info("Gerando análise de período para usuário {} entre {} e {}", usuarioId, inicio, fim);

        var dataInicio = inicio.atStartOfDay();
        var dataFim = fim.atTime(23, 59, 59);

        return repository.agruparPorData(usuarioId, dataInicio, dataFim);
    }

    private Usuario buscarDestinatario(TransacaoDTO dados) {
        if (dados.tipo() != TipoTransacao.TRANSFERENCIA) {
            return null;
        }

        if (dados.destinatarioId() == null) {
            throw new IllegalArgumentException("Destinatário é obrigatório para transferências");
        }

        if (dados.usuarioId().equals(dados.destinatarioId())) {
            throw new IllegalArgumentException("Remetente e destinatário não podem ser iguais");
        }

        return usuarioRepository.findById(dados.destinatarioId())
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));
    }

    private BigDecimal obterTaxaCambio(String moeda) {
        if (moeda == null || moeda.equalsIgnoreCase("BRL")) {
            return BigDecimal.ONE;
        }

        try {
            var cambio = brasilApiClient.buscarCotacao(moeda);
            return cambio != null ? cambio.valor() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Falha ao buscar cotação para moeda {}, salvando com taxa 0. Erro: {}", moeda, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private void notificarKafka(Transacao transacao) {
        try {
            transacaoProducer.enviarEvento(transacao);
            log.info("Evento de transação (ID: {}) enviado para o Kafka", transacao.getId());
        } catch (Exception e) {
            log.error("Erro ao enviar evento para o Kafka", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AnaliseCategoriaDTO> analisarPorCategoria(UUID usuarioId, LocalDate inicio, LocalDate fim) {
        var dataInicio = inicio.atStartOfDay();
        var dataFim = fim.atTime(23, 59, 59);

        return repository.agruparPorCategoria(usuarioId, dataInicio, dataFim);
    }
}