package com.gabriel.desafio.beca.api.infra.messaging;

import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.gabriel.desafio.beca.api.domain.repository.TransacaoRepository;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class TransacaoConsumer {

    @Autowired
    private TransacaoRepository repository;

    @Autowired
    private MockSaldoClient mockSaldoClient;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = ".dlq"
    )
    @KafkaListener(topics = "transaction.requested", groupId = "beca-processador-transacao")
    @Transactional
    public void processarTransacao(Transacao transacaoPayload) {
        System.out.println("PROCESSOR: Recebida transação ID: " + transacaoPayload.getId());

        Transacao transacao = repository.findById(transacaoPayload.getId())
                .orElseThrow(() -> new RuntimeException("Transação não encontrada no banco"));

        try {
            validarSaldoEAtualizarMock(transacao);

            transacao.setStatus(StatusTransacao.APPROVED);
            System.out.println("PROCESSOR: Transação APROVADA ✅");

        } catch (IllegalArgumentException e) {
            transacao.setStatus(StatusTransacao.REJECTED);
            System.out.println("PROCESSOR: Transação REJEITADA ❌ (" + e.getMessage() + ")");
        } catch (Exception e) {
            throw e;
        }

        repository.save(transacao);
    }

    private void validarSaldoEAtualizarMock(Transacao transacao) {
        String remetenteId = transacao.getUsuario().getId().toString();
        BigDecimal valor = transacao.getValor();

        if (transacao.getTipo() == TipoTransacao.SAQUE) {
            BigDecimal saldoAtual = mockSaldoClient.buscarSaldo(remetenteId);
            if (saldoAtual.compareTo(valor) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente");
            }
            mockSaldoClient.atualizarSaldo(remetenteId, saldoAtual.subtract(valor));
        }

        else if (transacao.getTipo() == TipoTransacao.DEPOSITO) {
            BigDecimal saldoAtual = mockSaldoClient.buscarSaldo(remetenteId);
            mockSaldoClient.atualizarSaldo(remetenteId, saldoAtual.add(valor));
        }

        else if (transacao.getTipo() == TipoTransacao.TRANSFERENCIA) {
            BigDecimal saldoRemetente = mockSaldoClient.buscarSaldo(remetenteId);
            if (saldoRemetente.compareTo(valor) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para transferência");
            }
            mockSaldoClient.atualizarSaldo(remetenteId, saldoRemetente.subtract(valor));

            if (transacao.getDestinatario() != null) {
                String destinatarioId = transacao.getDestinatario().getId().toString();
                BigDecimal saldoDestinatario = mockSaldoClient.buscarSaldo(destinatarioId);

                mockSaldoClient.atualizarSaldo(destinatarioId, saldoDestinatario.add(valor));

                System.out.println("TRANSFERENCIA: Deu " + valor + " para usuário " + destinatarioId);
            }
        }
    }

    @DltHandler
    public void processarDLQ(Transacao transacao, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        System.err.println("DLQ ALERTA 🚨: Transação " + transacao.getId() + " falhou definitivamente e foi para a DLQ: " + topic);

        Transacao t = repository.findById(transacao.getId()).orElse(null);
        if (t != null) {
            t.setStatus(StatusTransacao.ERROR);
            repository.save(t);
        }
    }
}