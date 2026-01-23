package com.gabriel.desafio.beca.api.domain.transacao;

import com.gabriel.desafio.beca.api.domain.user.Usuario;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_transacoes")
public class Transacao implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoTransacao tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusTransacao status;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "taxa_cambio")
    private BigDecimal taxaCambio;

    public Transacao() {}

    public Transacao(BigDecimal valor, TipoTransacao tipo, Usuario usuario, BigDecimal taxaCambio) {
        this.valor = valor;
        this.tipo = tipo;
        this.usuario = usuario;
        this.data = LocalDateTime.now();
        this.taxaCambio = taxaCambio;
        this.status = StatusTransacao.PENDING;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getData() { return data; }
    public TipoTransacao getTipo() { return tipo; }
    public Usuario getUsuario() { return usuario; }
    public BigDecimal getTaxaCambio() { return taxaCambio; }
    public StatusTransacao getStatus() { return status; }
    public void setStatus(StatusTransacao status) { this.status = status; }
}