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
import java.util.UUID;

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
                .orElseThrow(() -> new RuntimeException("Usuário remetente não encontrado!"));

        System.out.println("--- Recebendo solicitação de Transação para: " + usuario.getNome() + " ---");

        Usuario destinatario = null;
        if (dados.tipo() == TipoTransacao.TRANSFERENCIA) {
            if (dados.destinatarioId() == null) {
                throw new IllegalArgumentException("Para transferência, o ID do destinatário é obrigatório!");
            }
            if (dados.usuarioId().equals(dados.destinatarioId())) {
                throw new IllegalArgumentException("Não é possível transferir para a mesma conta!");
            }
            destinatario = usuarioRepository.findById(dados.destinatarioId())
                    .orElseThrow(() -> new RuntimeException("Usuário destinatário não encontrado!"));
        }

        BigDecimal cotacaoAtual = BigDecimal.ZERO;
        try {
            var cambioDTO = brasilApiClient.buscarCotacaoDolar();
            if (cambioDTO != null) {
                cotacaoAtual = cambioDTO.valor();
            }
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível buscar cotação do dólar (Seguindo fluxo).");
        }

        Transacao novaTransacao = new Transacao(
                dados.valor(),
                dados.tipo(),
                usuario,
                destinatario,
                cotacaoAtual
        );
        novaTransacao.setStatus(StatusTransacao.PENDING);

        Transacao transacaoSalva = repository.save(novaTransacao);

        try {
            transacaoProducer.enviarEvento(transacaoSalva);
            System.out.println("API: Evento enviado para o Kafka. ID: " + transacaoSalva.getId());
        } catch (Exception e) {
            System.err.println("CRÍTICO: Erro ao enviar para o Kafka: " + e.getMessage());
        }

        return transacaoSalva;
    }

    public BigDecimal consultarSaldo(UUID usuarioId) {
        return mockSaldoClient.buscarSaldo(usuarioId.toString());
    }

    public ExtratoDTO buscarExtrato(UUID usuarioId) {
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