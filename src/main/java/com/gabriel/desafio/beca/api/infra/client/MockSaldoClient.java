package com.gabriel.desafio.beca.api.infra.client;

import com.gabriel.desafio.beca.api.application.dto.SaldoExternoDTO;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.List;

@Component
public class MockSaldoClient {

    private final RestClient restClient;
    private final String RESOURCE = "/api/v1/saldos/contas";

    public MockSaldoClient() {
        String baseUrl = "https://6971044078fec16a63ffca46.mockapi.io";
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void criarConta(String usuarioId, BigDecimal saldoInicial) {
        try {
            SaldoExternoDTO novaConta = new SaldoExternoDTO(null, usuarioId, saldoInicial);

            restClient.post()
                    .uri(RESOURCE)
                    .body(novaConta)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("DEBUG: Conta criada no MockAPI com saldo R$ " + saldoInicial);
        } catch (Exception e) {
            System.err.println("ERRO MOCKAPI POST: " + e.getMessage());
        }
    }

    public BigDecimal buscarSaldo(String usuarioId) {
        try {
            List<SaldoExternoDTO> contas = restClient.get()
                    .uri(RESOURCE + "?usuarioId=" + usuarioId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SaldoExternoDTO>>() {});

            if (contas != null && !contas.isEmpty()) {
                return contas.get(0).saldo();
            }
            return BigDecimal.ZERO;
        } catch (Exception e) {
            System.err.println("ERRO MOCKAPI GET: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}