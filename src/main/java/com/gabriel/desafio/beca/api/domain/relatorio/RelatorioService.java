package com.gabriel.desafio.beca.api.domain.relatorio;

import com.gabriel.desafio.beca.api.domain.transacao.ExtratoDTO;
import com.gabriel.desafio.beca.api.domain.transacao.Transacao;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class RelatorioService {

    public byte[] gerarExtratoPdf(ExtratoDTO extrato) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            document.addTitle("Extrato Bancário - Banco Beca");
            document.addAuthor("Sistema Banco Beca");

            Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph titulo = new Paragraph("Extrato Bancário", fonteTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            Font fonteDados = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);
            document.add(new Paragraph("Cliente: " + extrato.usuario(), fonteDados));
            document.add(new Paragraph("Saldo Atual (MockAPI): R$ " + extrato.saldoAtual(), fonteDados));
            document.add(new Paragraph("Data de Emissão: " + java.time.LocalDate.now(), fonteDados));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 2f});

            addTableHeader(table, "ID");
            addTableHeader(table, "Data");
            addTableHeader(table, "Tipo");
            addTableHeader(table, "Valor (R$)");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            for (Transacao t : extrato.transacoes()) {
                table.addCell(t.getId().toString().substring(0, 8) + "...");
                table.addCell(t.getData().format(formatter));
                table.addCell(t.getTipo().toString());

                Font fonteValor = t.getTipo().toString().equals("DEPOSITO") ?
                        FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0, 100, 0)) :
                        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.RED);

                table.addCell(new Paragraph(t.getValor().toString(), fonteValor));
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
        header.setPhrase(new Phrase(headerTitle));
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }
}