package com.gabriel.desafio.beca.api.domain.analise;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_total_gastos")
public class TotalGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID usuarioId;
    private LocalDate data;
    private BigDecimal valorTotal;

    @Deprecated
    public TotalGasto() {}

    public TotalGasto(UUID usuarioId, LocalDate data, BigDecimal valorTotal) {
        this.usuarioId = usuarioId;
        this.data = data;
        this.valorTotal = valorTotal;
    }

    public void somarValor(BigDecimal valor) {
        this.valorTotal = this.valorTotal.add(valor);
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public LocalDate getData() { return data; }
    public BigDecimal getValorTotal() { return valorTotal; }
}