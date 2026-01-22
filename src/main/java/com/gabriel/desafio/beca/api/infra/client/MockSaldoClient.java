package com.gabriel.desafio.beca.api.infra.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Map;

@Component
public class MockSaldoClient {

    private final RestClient restClient;

    public MockSaldoClient() {
        String baseUrl = "https://6971044078fec16a63ffca46.mockapi.io";
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public BigDecimal buscarSaldo(String usuarioId) {
        try {
            var resposta = restClient.get()
                    .uri("/api/v1/saldos/saldo/1")
                    .retrieve()
                    .body(SaldoExternoDTO.class);
            return resposta.saldo();
        } catch (Exception e) {
            System.err.println("ERRO MOCKAPI GET: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    public void atualizarSaldo(String usuarioId, BigDecimal novoSaldo) {
        try {
            Map<String, BigDecimal> jsonEnvio = Map.of("saldo", novoSaldo);

            restClient.put()
                    .uri("/api/v1/saldos/saldo/1")
                    .body(jsonEnvio)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("DEBUG: Saldo atualizado na MockAPI para: " + novoSaldo);

        } catch (Exception e) {
            System.err.println("ERRO MOCKAPI PUT: " + e.getMessage());
        }
    }
}