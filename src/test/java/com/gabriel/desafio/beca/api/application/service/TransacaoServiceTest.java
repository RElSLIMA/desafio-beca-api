package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.application.dto.*;
import com.gabriel.desafio.beca.api.domain.model.CategoriaTransacao;
import com.gabriel.desafio.beca.api.domain.model.StatusTransacao;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import com.gabriel.desafio.beca.api.domain.repository.TransacaoRepository;
import com.gabriel.desafio.beca.api.domain.repository.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.messaging.TransacaoProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    @DisplayName("Deve registrar um DEPÓSITO com sucesso (Moeda BRL)")
    void deveRegistrarDeposito() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("100.00"),
                TipoTransacao.DEPOSITO,
                usuarioId,
                null,
                null,
                "BRL"
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertEquals(TipoTransacao.DEPOSITO, resultado.getTipo());
        assertEquals(StatusTransacao.PENDING, resultado.getStatus());
        assertEquals(CategoriaTransacao.OUTROS, resultado.getCategoria());
        verify(transacaoProducer).enviarEvento(any(Transacao.class));
    }

    @Test
    @DisplayName("Deve registrar um SAQUE com sucesso (Moeda USD)")
    void deveRegistrarSaque() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("50.00"),
                TipoTransacao.SAQUE,
                usuarioId,
                CategoriaTransacao.LAZER,
                null,
                "USD"
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(brasilApiClient.buscarCotacao("USD")).thenReturn(new CambioDTO("USD", new BigDecimal("5.50")));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertEquals(TipoTransacao.SAQUE, resultado.getTipo());
        assertEquals(CategoriaTransacao.LAZER, resultado.getCategoria());
        assertEquals(new BigDecimal("5.50"), resultado.getTaxaCambio());
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
                CategoriaTransacao.OUTROS,
                destinatarioId,
                "BRL"
        );

        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));
        when(usuarioRepository.findById(destinatarioId)).thenReturn(Optional.of(destinatario));

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
    @DisplayName("Gestão Financeira: Deve PERMITIR saque mesmo sem saldo (Saldo Negativo)")
    void devePermitirSaqueSemSaldo() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gastador");

        TransacaoDTO dados = new TransacaoDTO(
                new BigDecimal("500000.00"),
                TipoTransacao.SAQUE,
                usuarioId,
                CategoriaTransacao.ALIMENTACAO,
                null,
                "BRL"
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        when(repository.save(any(Transacao.class))).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        assertDoesNotThrow(() -> service.registrar(dados));
        verify(transacaoProducer).enviarEvento(any());
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
                null,
                null,
                "BRL"
        );

        when(usuarioRepository.findById(remetenteId)).thenReturn(Optional.of(remetente));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(dados));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Resiliência: Deve registrar com taxa 0 se BrasilAPI falhar")
    void deveRegistrarMesmoSemBrasilApi() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = criarUsuarioMock(usuarioId, "Gabriel");

        TransacaoDTO dados = new TransacaoDTO(
                BigDecimal.TEN,
                TipoTransacao.SAQUE,
                usuarioId,
                CategoriaTransacao.EDUCACAO,
                null,
                "EUR"
        );

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(brasilApiClient.buscarCotacao("EUR")).thenThrow(new RuntimeException("API Offline"));

        when(repository.save(any())).thenAnswer(i -> {
            Transacao t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
            return t;
        });

        Transacao resultado = service.registrar(dados);

        assertNotNull(resultado);
        assertEquals(BigDecimal.ZERO, resultado.getTaxaCambio());
        verify(transacaoProducer).enviarEvento(any());
    }

    // --- TESTES DE SALDO E EXTRATO ---

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

    // --- TESTES DE RELATÓRIOS E ANÁLISES ---

    @Test
    @DisplayName("Deve agrupar dados por DATA convertendo horário")
    void deveAnalisarPeriodoCorretamente() {
        UUID usuarioId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 5);

        List<AnaliseDiariaDTO> mockResultado = List.of(
                new AnaliseDiariaDTO(LocalDate.of(2026, 1, 2), new BigDecimal("150.00"))
        );

        when(repository.agruparPorData(
                eq(usuarioId),
                eq(inicio.atStartOfDay()),
                eq(fim.atTime(23, 59, 59))
        )).thenReturn(mockResultado);

        List<AnaliseDiariaDTO> resultado = service.analisarPeriodo(usuarioId, inicio, fim);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(repository).agruparPorData(any(), any(), any());
    }

    @Test
    @DisplayName("Deve agrupar dados por CATEGORIA convertendo horário")
    void deveAnalisarPorCategoriaCorretamente() {
        UUID usuarioId = UUID.randomUUID();
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 5);

        List<AnaliseCategoriaDTO> mockResultado = List.of(
                new AnaliseCategoriaDTO(CategoriaTransacao.ALIMENTACAO, new BigDecimal("200.00")),
                new AnaliseCategoriaDTO(CategoriaTransacao.LAZER, new BigDecimal("100.00"))
        );

        when(repository.agruparPorCategoria(
                eq(usuarioId),
                eq(inicio.atStartOfDay()),
                eq(fim.atTime(23, 59, 59))
        )).thenReturn(mockResultado);

        List<AnaliseCategoriaDTO> resultado = service.analisarPorCategoria(usuarioId, inicio, fim);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(CategoriaTransacao.ALIMENTACAO, resultado.get(0).categoria());

        verify(repository).agruparPorCategoria(any(), any(), any());
    }

    // --- MÉTODOS AUXILIARES ---

    private Usuario criarUsuarioMock(UUID id, String nome) {
        Usuario u = new Usuario(nome, "email@teste.com", "123", "000");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}