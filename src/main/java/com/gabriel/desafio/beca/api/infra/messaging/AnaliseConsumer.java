package com.gabriel.desafio.beca.api.infra.messaging;

import com.gabriel.desafio.beca.api.domain.model.TotalGasto;
import com.gabriel.desafio.beca.api.domain.repository.TotalGastoRepository;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class AnaliseConsumer {

    @Autowired
    private TotalGastoRepository repository;

    @KafkaListener(topics = "transacoes-realizadas", groupId = "grupo-analise-despesas-v2")
    public void receberEvento(Transacao transacao) {

        if (transacao.getTipo() == TipoTransacao.DEPOSITO) {
            return;
        }

        System.out.println("ANALISTA: Processando gasto de R$ " + transacao.getValor());

        UUID usuarioId = transacao.getUsuario().getId();
        LocalDate hoje = LocalDate.now();

        TotalGasto totalDoDia = repository.findByUsuarioIdAndData(usuarioId, hoje)
                .orElse(new TotalGasto(usuarioId, hoje, BigDecimal.ZERO));

        totalDoDia.somarValor(transacao.getValor());

        repository.save(totalDoDia);

        System.out.println("ANALISTA: Total atualizado para R$ " + totalDoDia.getValorTotal());
    }
}