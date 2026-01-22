package com.gabriel.desafio.beca.api.infra.kafka;

import com.gabriel.desafio.beca.api.domain.transacao.Transacao;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransacaoProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransacaoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void enviarEvento(Transacao transacao) {
        kafkaTemplate.send("transacoes-realizadas", transacao);

        System.out.println("KAFKA: Evento de transação enviado! ID: " + transacao.getId());
    }
}