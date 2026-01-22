package com.gabriel.desafio.beca.api.domain.transacao;

import com.gabriel.desafio.beca.api.domain.user.Usuario;
import com.gabriel.desafio.beca.api.domain.user.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.BrasilApiClient;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import com.gabriel.desafio.beca.api.infra.kafka.TransacaoProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransacaoService {

    @Autowired
    private TransacaoRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BrasilApiClient brasilApiClient;

    @Autowired
    private MockSaldoClient mockSaldoClient;

    @Autowired
    private TransacaoProducer transacaoProducer;

    @Transactional
    public Transacao registrar(TransacaoDTO dados) {
        Usuario usuario = usuarioRepository.findById(dados.usuarioId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (dados.tipo() == TipoTransacao.SAQUE || dados.tipo() == TipoTransacao.TRANSFERENCIA) {
            BigDecimal saldoDisponivel = mockSaldoClient.buscarSaldo(usuario.getId().toString());

            if (saldoDisponivel.compareTo(dados.valor()) < 0) {
                throw new RuntimeException("Saldo insuficiente (Mock)! Disponível: R$ " + saldoDisponivel);
            }

            BigDecimal novoSaldo = saldoDisponivel.subtract(dados.valor());
            mockSaldoClient.atualizarSaldo(usuario.getId().toString(), novoSaldo);
        }

        if (dados.tipo() == TipoTransacao.DEPOSITO) {
            BigDecimal saldoDisponivel = mockSaldoClient.buscarSaldo(usuario.getId().toString());
            BigDecimal novoSaldo = saldoDisponivel.add(dados.valor());
            mockSaldoClient.atualizarSaldo(usuario.getId().toString(), novoSaldo);
        }

        BigDecimal cotacaoAtual = BigDecimal.ZERO;
        var cambioDTO = brasilApiClient.buscarCotacaoDolar();
        if (cambioDTO != null) {
            cotacaoAtual = cambioDTO.valor();
        }

        Transacao novaTransacao = new Transacao(
                dados.valor(),
                dados.tipo(),
                usuario,
                cotacaoAtual
        );
        Transacao transacaoSalva = repository.save(novaTransacao);

        try {
            transacaoProducer.enviarEvento(transacaoSalva);
        } catch (Exception e) {
            System.err.println("Erro ao enviar para o Kafka (mas a transação foi salva): " + e.getMessage());
        }

        return transacaoSalva;
    }

    public BigDecimal consultarSaldo(java.util.UUID usuarioId) {
        return mockSaldoClient.buscarSaldo(usuarioId.toString());
    }

    public ExtratoDTO buscarExtrato(java.util.UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        BigDecimal saldo = mockSaldoClient.buscarSaldo(usuarioId.toString());
        List<Transacao> historico = repository.findByUsuarioId(usuarioId);

        return new ExtratoDTO(
                usuario.getNome(),
                saldo,
                historico
        );
    }
}