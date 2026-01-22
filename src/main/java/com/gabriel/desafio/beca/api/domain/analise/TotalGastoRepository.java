package com.gabriel.desafio.beca.api.domain.analise;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TotalGastoRepository extends JpaRepository<TotalGasto, UUID> {
    Optional<TotalGasto> findByUsuarioIdAndData(UUID usuarioId, LocalDate data);
}