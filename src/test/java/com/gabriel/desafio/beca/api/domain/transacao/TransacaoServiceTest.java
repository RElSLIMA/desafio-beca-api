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
    @DisplayName("Cenário Crítico 1: Deve salvar transação como PENDING e enviar para Kafka")
    void deveRegistrarTransacaoComSucesso() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioMock = criarUsuarioMock(usuarioId);

        TransacaoDTO dadosEntrada = new TransacaoDTO(
                new BigDecimal("100.00"),
                TipoTransacao.DEPOSITO,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioMock));
        when(brasilApiClient.buscarCotacaoDolar()).thenReturn(new CambioDTO("USD", new BigDecimal("5.50")));

        when(repository.save(any(Transacao.class))).thenAnswer(invocation -> {
            Transacao t = invocation.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dadosEntrada);

        assertNotNull(resultado.getId());
        assertEquals(StatusTransacao.PENDING, resultado.getStatus());
        assertEquals(new BigDecimal("5.50"), resultado.getTaxaCambio());

        verify(transacaoProducer, times(1)).enviarEvento(any(Transacao.class));
    }

    @Test
    @DisplayName("Cenário Crítico 2: Deve funcionar mesmo se a BrasilAPI estiver fora do ar")
    void deveRegistrarMesmoSemBrasilAPI() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioMock = criarUsuarioMock(usuarioId);

        TransacaoDTO dadosEntrada = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.SAQUE,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioMock));
        when(brasilApiClient.buscarCotacaoDolar()).thenThrow(new RuntimeException("API Fora do Ar"));
        when(repository.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));

        Transacao resultado = service.registrar(dadosEntrada);

        assertNotNull(resultado);
        assertEquals(BigDecimal.ZERO, resultado.getTaxaCambio());
        verify(transacaoProducer, times(1)).enviarEvento(any(Transacao.class));
    }

    @Test
    @DisplayName("Cenário Crítico 3: Não deve enviar ao Kafka se usuário não existir")
    void deveFalharSeUsuarioInexistente() {
        UUID idInexistente = UUID.randomUUID();

        TransacaoDTO dadosEntrada = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.DEPOSITO,
                idInexistente,
                null
        );

        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.registrar(dadosEntrada));

        verify(transacaoProducer, never()).enviarEvento(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Cenário 4: Deve registrar TRANSFERENCIA com sucesso (Remetente e Destinatário)")
    void deveRegistrarTransferenciaComSucesso() {
        UUID remetenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();

        Usuario remetente = criarUsuarioMock(remetenteId);
        Usuario destinatario = criarUsuarioMock(destinatarioId);

        TransacaoDTO dadosEntrada = new TransacaoDTO(
                new BigDecimal("50.00"),
                TipoTransacao.TRANSFERENCIA,
                remetenteId,
                destinatarioId
        );

        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(repository.save(any(Transacao.class))).thenAnswer(i -> i.getArgument(0));

        Transacao resultado = service.registrar(dadosEntrada);

        assertNotNull(resultado);
        assertEquals(destinatario, resultado.getDestinatario());
        verify(transacaoProducer, times(1)).enviarEvento(any(Transacao.class));
    }

    private Usuario criarUsuarioMock(UUID id) {
        Usuario u = new Usuario("Gabriel", "gabriel@email.com", "123", "000.000.000-00");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}