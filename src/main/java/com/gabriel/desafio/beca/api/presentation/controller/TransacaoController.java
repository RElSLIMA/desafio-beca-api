package com.gabriel.desafio.beca.api.presentation.controller;

import com.gabriel.desafio.beca.api.application.dto.ExtratoDTO;
import com.gabriel.desafio.beca.api.application.dto.TransacaoDTO;
import com.gabriel.desafio.beca.api.application.dto.TransacaoResponseDTO;
import com.gabriel.desafio.beca.api.application.service.RelatorioService;
import com.gabriel.desafio.beca.api.application.service.TransacaoService;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/transacoes")
public class TransacaoController {

    @Autowired
    private TransacaoService service;

    @Autowired
    private RelatorioService relatorioService;

    @PostMapping
    public ResponseEntity<TransacaoResponseDTO> registrar(@RequestBody @Valid TransacaoDTO dados) {
        Transacao transacao = service.registrar(dados);

        var response = new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getValor(),
                transacao.getTipo(),
                transacao.getStatus(),
                transacao.getTaxaCambio(),
                transacao.getData()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/saldo")
    public ResponseEntity<BigDecimal> consultarSaldo(@RequestParam UUID usuarioId) {
        BigDecimal saldo = service.consultarSaldo(usuarioId);
        return ResponseEntity.ok(saldo);
    }

    @GetMapping("/extrato")
    public ResponseEntity<ExtratoDTO> consultarExtrato(@RequestParam UUID usuarioId) {
        ExtratoDTO extrato = service.buscarExtrato(usuarioId);
        return ResponseEntity.ok(extrato);
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportarExtratoPdf(@RequestParam UUID usuarioId) {
        var extrato = service.buscarExtrato(usuarioId);
        byte[] pdfBytes = relatorioService.gerarExtratoPdf(extrato);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extrato-" + usuarioId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}