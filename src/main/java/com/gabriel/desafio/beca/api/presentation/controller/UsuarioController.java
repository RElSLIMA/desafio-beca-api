package com.gabriel.desafio.beca.api.presentation.controller;

import com.gabriel.desafio.beca.api.application.dto.UsuarioDTO;
import com.gabriel.desafio.beca.api.application.dto.UsuarioResponseDTO;
import com.gabriel.desafio.beca.api.application.service.UsuarioService;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService service;

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> criar(@RequestBody @Valid UsuarioDTO dados) {
        Usuario usuario = service.criarUsuario(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(usuario));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> listar() {
        List<Usuario> usuarios = service.listarTodos();

        List<UsuarioResponseDTO> response = usuarios.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable UUID id) {
        Usuario usuario = service.buscarPorId(id);
        return ResponseEntity.ok(toDTO(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@PathVariable UUID id, @RequestBody UsuarioDTO dados) {
        Usuario usuario = service.atualizarUsuario(id, dados);
        return ResponseEntity.ok(toDTO(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadUsuarios(@RequestParam("file") MultipartFile file) {
        service.salvarUsuariosViaExcel(file);
        return ResponseEntity.ok("Upload realizado com sucesso! Verifique os logs para detalhes.");
    }

    private UsuarioResponseDTO toDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf()
        );
    }
}