package com.gabriel.desafio.beca.api.domain.user;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @PostMapping
    public ResponseEntity<Usuario> criar(@RequestBody @Valid UsuarioDTO dados) {
        Usuario usuarioCriado = service.criarUsuario(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizar(@PathVariable UUID id, @RequestBody UsuarioDTO dados) {
        return ResponseEntity.ok(service.atualizarUsuario(id, dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadUsuarios(@RequestParam("file") MultipartFile file) {
        service.salvarUsuariosViaExcel(file);
        return ResponseEntity.ok("Upload realizado com sucesso! Verifique os logs para erros individuais.");
    }
}