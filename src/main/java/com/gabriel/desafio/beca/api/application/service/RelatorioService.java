package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.application.dto.ExtratoDTO;
import com.gabriel.desafio.beca.api.domain.model.TipoTransacao;
import com.gabriel.desafio.beca.api.domain.model.Transacao;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class RelatorioService {

    public byte[] gerarExtratoPdf(ExtratoDTO extrato) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);

            document.open();

            document.addTitle("Extrato Financeiro - Sistema de Gestão Financeira Beca");
            document.addAuthor("Sistema de Gestão Financeira Beca");

            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph titulo = new Paragraph("Relatório de Gestão Financeira", fonteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            Font fonteDados = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
            DateTimeFormatter formatoDataEmissao = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            document.add(new Paragraph("Cliente: " + extrato.usuario(), fonteDados));
            document.add(new Paragraph("Saldo Bancário: R$ " + extrato.saldoAtual(), fonteDados));
            document.add(new Paragraph("Data de Emissão: " + LocalDate.now().format(formatoDataEmissao), fonteDados));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.8f, 2f, 1.5f, 3.5f, 1.8f, 2f});

            addTableHeader(table, "ID Transação");
            addTableHeader(table, "Data");
            addTableHeader(table, "Categoria");
            addTableHeader(table, "Tipo / Detalhes");
            addTableHeader(table, "Cotação");
            addTableHeader(table, "Valor (R$)");

            DateTimeFormatter formatterTabela = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Transacao t : extrato.transacoes()) {

                Font fonteValor;
                String detalhesDisplay;
                String idTransacaoDisplay = t.getId().toString();
                String nomeDonoExtrato = extrato.usuario();
                String nomeRemetente = t.getUsuario().getNome();

                if (t.getTipo() == TipoTransacao.DEPOSITO) {
                    fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0, 100, 0));
                    detalhesDisplay = "DEPÓSITO";

                } else if (t.getTipo() == TipoTransacao.SAQUE) {
                    fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.RED);
                    detalhesDisplay = "SAQUE";

                } else if (t.getTipo() == TipoTransacao.COMPRA) {
                    fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(200, 80, 0));
                    detalhesDisplay = "COMPRA";

                } else {
                    boolean fuiEuQueMandei = nomeRemetente.equalsIgnoreCase(nomeDonoExtrato);

                    if (fuiEuQueMandei) {
                        fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLUE);
                        String idDestinatario = (t.getDestinatario() != null) ? t.getDestinatario().getId().toString() : "N/A";
                        detalhesDisplay = "TRANSF. ENVIADA\nPara: " + idDestinatario;

                    } else {
                        Color amareloLegivel = new Color(204, 153, 0);
                        fonteValor = FontFactory.getFont(FontFactory.HELVETICA, 10, amareloLegivel);
                        String idRemetente = t.getUsuario().getId().toString();
                        detalhesDisplay = "TRANSF. RECEBIDA\nDe: " + idRemetente;
                    }
                }

                Font fonteId = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
                table.addCell(new Paragraph(idTransacaoDisplay, fonteId));
                table.addCell(new Paragraph(t.getData().format(formatterTabela), FontFactory.getFont(FontFactory.HELVETICA, 9)));
                String catDisplay = (t.getCategoria() != null) ? t.getCategoria().toString() : "OUTROS";
                table.addCell(new Paragraph(catDisplay, FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Paragraph(detalhesDisplay, FontFactory.getFont(FontFactory.HELVETICA, 9)));

                String taxaDisplay;
                BigDecimal taxa = t.getTaxaCambio();
                String moeda = (t.getMoeda() != null) ? t.getMoeda() : "BRL";

                if (taxa == null || taxa.compareTo(BigDecimal.ONE) == 0 || taxa.compareTo(BigDecimal.ZERO) == 0) {
                    taxaDisplay = "BRL (1:1)";
                } else {
                    taxaDisplay = moeda + " " + taxa.toString();
                }
                table.addCell(new Paragraph(taxaDisplay, FontFactory.getFont(FontFactory.HELVETICA, 9)));
                table.addCell(new Paragraph("R$ " + t.getValor().toString(), fonteValor));
            }

            document.add(table);
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(Color.LIGHT_GRAY);
        header.setBorderWidth(1);
        header.setPhrase(new Phrase(headerTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        header.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(header);
    }
}