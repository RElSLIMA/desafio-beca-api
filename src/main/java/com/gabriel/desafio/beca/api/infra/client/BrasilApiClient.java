package com.gabriel.desafio.beca.api.infra.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class BrasilApiClient {

    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BrasilApiClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://brasilapi.com.br/api")
                .build();
    }

    public CambioDTO buscarCotacaoDolar() {
        LocalDate dataParaBuscar = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            try {
                String dataFormatada = dataParaBuscar.format(formatter);
                System.out.println("Tentando buscar cotação para data: " + dataFormatada);

                JsonNode json = restClient.get()
                        .uri("/cambio/v1/cotacao/USD/{data}", dataFormatada)
                        .retrieve()
                        .body(JsonNode.class);

                JsonNode listaCotacoes = json.get("cotacoes");
                if (listaCotacoes.isArray() && !listaCotacoes.isEmpty()) {
                    JsonNode ultimaCotacao = listaCotacoes.get(listaCotacoes.size() - 1);
                    BigDecimal valorDolar = new BigDecimal(ultimaCotacao.get("cotacao_venda").asText());

                    System.out.println("Cotação encontrada com sucesso!");
                    return new CambioDTO("Dólar", valorDolar);
                }

            } catch (Exception e) {
                System.out.println("Sem cotação para " + dataParaBuscar + ". Tentando dia anterior...");
            }

            dataParaBuscar = dataParaBuscar.minusDays(1);
        }

        System.err.println("Falha ao buscar cotação em todos os dias recentes. Usando backup.");
        return new CambioDTO("Dólar (Backup)", new BigDecimal("5.70"));
    }
}