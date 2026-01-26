package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.application.dto.CambioDTO;
import com.gabriel.desafio.beca.api.application.dto.ExtratoDTO;
import com.gabriel.desafio.beca.api.application.dto.TransacaoDTO;
import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import com.gabriel.desafio.beca.api.domain.repository.TransacaoRepository;
import com.gabriel.desafio.beca.api.domain.repository.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.messaging.TransacaoProducer;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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

    // --- TESTES DE REGISTRO (POST /transacoes) ---

    @Test
    @DisplayName("Deve registrar um DEPÓSITO com sucesso")
    void deveRegistrarDeposito() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("100.00"),
                TipoTransacao.DEPOSITO,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(brasilApiClient.buscarCotacaoDolar()).thenReturn(new CambioDTO("USD", BigDecimal.valueOf(5.50)));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertEquals(TipoTransacao.DEPOSITO, resultado.getTipo());
        assertEquals(StatusTransacao.PENDING, resultado.getStatus());
        verify(transacaoProducer).enviarEvento(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve registrar um SAQUE com sucesso")
    void deveRegistrarSaque() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("50.00"),
                TipoTransacao.SAQUE,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        // Mock do Saldo positivo
        when(mockSaldoClient.buscarSaldo(usuarioId.toString())).thenReturn(new BigDecimal("100.00"));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertEquals(TipoTransacao.SAQUE, resultado.getTipo());
        verify(transacaoProducer).enviarEvento(resultado);
    }

    @Test
    @DisplayName("Deve registrar uma TRANSFERÊNCIA com sucesso")
    void deveRegistrarTransferencia() {
        UUID remetenteId = UUID.randomUUID();
        UUID destinatarioId = UUID.randomUUID();

        Usuario remetente = criarUsuarioMock(remetenteId, "Remetente");
        Usuario destinatario = criarUsuarioMock(destinatarioId, "Destinatario");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("200.00"),
                TipoTransacao.TRANSFERENCIA,
                remetenteId,
                destinatarioId
        );

        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));
        when(mockSaldoClient.buscarSaldo(remetenteId.toString())).thenReturn(new BigDecimal("500.00"));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertEquals(TipoTransacao.TRANSFERENCIA, resultado.getTipo());
        assertNotNull(resultado.getDestinatario());
        verify(transacaoProducer).enviarEvento(resultado);
    }

    @Test
    @DisplayName("Erro: Transferência sem destinatário deve falhar")
    void deveFalharTransferenciaSemDestinatario() {
        UUID remetenteId = UUID.randomUUID();
        Usuario remetente = criarUsuarioMock(remetenteId, "Remetente");

        TransacaoDTO dados = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.TRANSFERENCIA,
                remetenteId,
                null
        );

        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(dados));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Erro: Transferência para a mesma conta deve falhar")
    void deveFalharTransferenciaMesmaConta() {
        UUID meuId = UUID.randomUUID();
        Usuario euMesmo = criarUsuarioMock(meuId, "Eu");

        TransacaoDTO dados = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.TRANSFERENCIA,
                meuId,
                meuId
        );

        when(usuarioRepository.findById(meuId)).thenReturn(Optional.of(euMesmo));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(dados));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Erro: Deve bloquear SAQUE sem saldo suficiente")
    void deveBloquearSaqueSemSaldo() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Pobre");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("50.00"),
                TipoTransacao.SAQUE,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        // Saldo ZERO
        when(mockSaldoClient.buscarSaldo(usuarioId.toString())).thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(dados));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Resiliência: Deve registrar mesmo se BrasilAPI falhar")
    void deveRegistrarMesmoSemBrasilApi() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.SAQUE,
                usuarioId,
                null
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(mockSaldoClient.buscarSaldo(usuarioId.toString())).thenReturn(new BigDecimal("100.00"));
        when(brasilApiClient.buscarCotacaoDolar()).thenThrow(new RuntimeException("API Offline"));

        when(repository.save(any())).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertNotNull(resultado);
        verify(transacaoProducer).enviarEvento(any());
    }

    // --- TESTES DE SALDO (GET /transacoes/saldo) ---

    @Test
    @DisplayName("Deve consultar saldo no MockSaldoClient")
    void deveConsultarSaldo() {
        UUID usuarioId = UUID.randomUUID();
        BigDecimal saldoEsperado = new BigDecimal("1500.00");

        when(mockSaldoClient.buscarSaldo(usuarioId.toString())).thenReturn(saldoEsperado);

        BigDecimal saldoRetornado = service.consultarSaldo(usuarioId);

        assertEquals(saldoEsperado, saldoRetornado);
        verify(mockSaldoClient).buscarSaldo(usuarioId.toString());
    }

    // --- TESTES DE EXTRATO (GET /transacoes/extrato) ---

    @Test
    @DisplayName("Deve buscar extrato com saldo e histórico completo")
    void deveBuscarExtratoComSucesso() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");
        BigDecimal saldoMock = new BigDecimal("500.00");
        List<Transacao> historicoMock = Collections.singletonList(new Transacao());

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(mockSaldoClient.buscarSaldo(usuarioId.toString())).thenReturn(saldoMock);

        when(repository.findHistoricoCompleto(usuarioId)).thenReturn(historicoMock);

        ExtratoDTO extrato = service.buscarExtrato(usuarioId);

        assertNotNull(extrato);
        assertEquals("Gabriel", extrato.usuario());
        assertEquals(saldoMock, extrato.saldoAtual());
        assertFalse(extrato.transacoes().isEmpty());
    }

    @Test
    @DisplayName("Erro: Buscar extrato de usuário inexistente deve falhar")
    void deveFalharExtratoUsuarioInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.buscarExtrato(idInexistente));
    }

    // --- MÉTODOS AUXILIARES ---

    private Usuario criarUsuarioMock(UUID id, String nome) {
        Usuario u = new Usuario(nome, "email@teste.com", "123", "000");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}