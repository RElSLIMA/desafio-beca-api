package com.gabriel.desafio.beca.api.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.gabriel.desafio.beca.api.application.dto.CambioDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class BrasilApiClient {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiClient.class);

    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BrasilApiClient(@Qualifier("brasilApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public CambioDTO buscarCotacao(String moeda) {
        LocalDate dataParaBuscar = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            String dataFormatada = dataParaBuscar.format(formatter);

            try {
                log.info("Tentando buscar cotação de {} para a data: {}", moeda, dataFormatada);

                JsonNode json = restClient.get()
                        .uri("/cambio/v1/cotacao/{moeda}/{data}", moeda, dataFormatada)
                        .retrieve()
                        .body(JsonNode.class);

                JsonNode listaCotacoes = json.get("cotacoes");

                if (listaCotacoes != null && listaCotacoes.isArray() && !listaCotacoes.isEmpty()) {
                    JsonNode ultimaCotacao = listaCotacoes.get(listaCotacoes.size() - 1);
                    BigDecimal valorCotacao = new BigDecimal(ultimaCotacao.get("cotacao_venda").asText());

                    log.info("Cotação encontrada com sucesso: R$ {}", valorCotacao);
                    return new CambioDTO(moeda, valorCotacao);
                }

            } catch (Exception e) {
                log.warn("Sem cotação para {}. Erro/Feriado. Tentando dia anterior...", dataFormatada);
            }

            dataParaBuscar = dataParaBuscar.minusDays(1);
        }

        log.error("Falha ao buscar cotação de {} após várias tentativas. Seguindo sem taxa.", moeda);
        return null;
    }
}