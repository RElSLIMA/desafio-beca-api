package com.gabriel.desafio.beca.api.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient brasilApiRestClient() {
        return RestClient.builder()
                .baseUrl("https://brasilapi.com.br/api")
                .build();
    }
}