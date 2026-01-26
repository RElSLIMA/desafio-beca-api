package com.gabriel.desafio.beca.api.presentation.controller;

import com.gabriel.desafio.beca.api.domain.model.TotalGasto;
import com.gabriel.desafio.beca.api.domain.repository.TotalGastoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/analise")
public class AnaliseController {

    @Autowired
    private TotalGastoRepository repository;

    @GetMapping("/gastos")
    public ResponseEntity<List<TotalGasto>> consultarGastos(@RequestParam UUID usuarioId) {
        var todos = repository.findAll();
        var filtrados = todos.stream().filter(t -> t.getUsuarioId().equals(usuarioId)).toList();

        return ResponseEntity.ok(filtrados);
    }
}