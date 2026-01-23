package com.gabriel.desafio.beca.api.infra.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    public void criarConta(String usuarioId) {
        try {
            SaldoExternoDTO novaConta = new SaldoExternoDTO(null, usuarioId, BigDecimal.ZERO);

            restClient.post()
                    .uri(RESOURCE)
                    .body(novaConta)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("DEBUG: Conta criada em " + RESOURCE + " para o usuário: " + usuarioId);
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

    public void atualizarSaldo(String usuarioId, BigDecimal novoSaldo) {
        try {
            List<SaldoExternoDTO> contas = restClient.get()
                    .uri(RESOURCE + "?usuarioId=" + usuarioId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<SaldoExternoDTO>>() {});

            if (contas == null || contas.isEmpty()) {
                System.err.println("ERRO: Registro não encontrado para usuário " + usuarioId);
                return;
            }

            String idMock = contas.get(0).id();

            Map<String, Object> jsonEnvio = Map.of(
                    "usuarioId", usuarioId,
                    "saldo", novoSaldo
            );

            restClient.put()
                    .uri(RESOURCE + "/" + idMock)
                    .body(jsonEnvio)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("DEBUG: Saldo atualizado para " + novoSaldo + " (ID Mock: " + idMock + ")");

        } catch (Exception e) {
            System.err.println("ERRO MOCKAPI PUT: " + e.getMessage());
        }
    }
}