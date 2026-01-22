package com.gabriel.desafio.beca.api.domain.transacao;

import com.gabriel.desafio.beca.api.domain.user.Usuario;
import com.gabriel.desafio.beca.api.domain.user.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.CambioDTO;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.kafka.TransacaoProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BrasilApiClient brasilApiClient;

    @Mock
    private MockSaldoClient mockSaldoClient;

    @Mock
    private TransacaoProducer transacaoProducer;

    @InjectMocks
    private TransacaoService service;

    @Test
    @DisplayName("Deve registrar um Depósito com sucesso")
    void deveRegistrarDepositoComSucesso() {
        UUID usuarioId = UUID.randomUUID();

        Usuario usuarioMock = new Usuario("Gabriel", "gabriel@email.com", "123", "000.000.000-00");

        ReflectionTestUtils.setField(usuarioMock, "id", usuarioId);

        TransacaoDTO dadosEntrada = new TransacaoDTO(
                new BigDecimal("100.00"),
                TipoTransacao.DEPOSITO,
                usuarioId
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioMock));

        when(mockSaldoClient.buscarSaldo(anyString())).thenReturn(new BigDecimal("50.00"));

        when(brasilApiClient.buscarCotacaoDolar()).thenReturn(new CambioDTO("Dólar", new BigDecimal("5.50")));

        when(repository.save(any(Transacao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transacao resultado = service.registrar(dadosEntrada);

        assertNotNull(resultado);
        assertEquals(new BigDecimal("100.00"), resultado.getValor());
        assertEquals(TipoTransacao.DEPOSITO, resultado.getTipo());
        assertEquals(new BigDecimal("5.50"), resultado.getTaxaCambio());

        verify(transacaoProducer, times(1)).enviarEvento(any(Transacao.class));
        verify(mockSaldoClient, times(1)).atualizarSaldo(anyString(), eq(new BigDecimal("150.00")));
    }
}