package com.gabriel.desafio.beca.api.application.service;

import com.gabriel.desafio.beca.api.application.dto.UsuarioDTO;
import com.gabriel.desafio.beca.api.domain.model.Usuario;
import com.gabriel.desafio.beca.api.domain.repository.UsuarioRepository;
import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MockSaldoClient mockSaldoClient;

    @InjectMocks
    private UsuarioService service;

    // --- TESTES DE CRIAÇÃO (POST) ---

    @Test
    @DisplayName("Deve criar usuário com sucesso")
    void deveCriarUsuarioComSucesso() {
        UsuarioDTO dados = new UsuarioDTO("Gabriel", "g@email.com", "123", "000");

        when(repository.existsByEmail(any())).thenReturn(false);
        when(repository.existsByCpf(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("HASH");
        when(repository.save(any())).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
            return u;
        });

        Usuario criado = service.criarUsuario(dados);
        assertNotNull(criado.getId());
        verify(mockSaldoClient).criarConta(anyString());
    }

    @Test
    @DisplayName("Deve falhar ao criar email duplicado")
    void deveFalharEmailDuplicado() {
        UsuarioDTO dados = new UsuarioDTO("Gabriel", "existente@email.com", "123", "000");
        when(repository.existsByEmail(dados.email())).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.criarUsuario(dados));
    }

    // --- TESTES DE BUSCA (GET) ---

    @Test
    @DisplayName("Deve buscar usuário por ID existente")
    void deveBuscarPorId() {
        UUID id = UUID.randomUUID();
        Usuario usuarioMock = new Usuario("Gabriel", "g@email.com", "123", "000");

        when(repository.findById(id)).thenReturn(Optional.of(usuarioMock));

        Usuario encontrado = service.buscarPorId(id);

        assertEquals("Gabriel", encontrado.getNome());
    }

    @Test
    @DisplayName("Deve lançar erro ao buscar ID inexistente")
    void deveFalharBuscarIdInexistente() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.buscarPorId(id));
    }

    @Test
    @DisplayName("Deve listar todos os usuários")
    void deveListarTodos() {
        List<Usuario> lista = List.of(new Usuario(), new Usuario());
        when(repository.findAll()).thenReturn(lista);

        List<Usuario> resultado = service.listarTodos();

        assertEquals(2, resultado.size());
    }

    // --- TESTES DE ATUALIZAÇÃO (PUT) ---

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void deveAtualizarUsuario() {
        UUID id = UUID.randomUUID();
        Usuario usuarioAntigo = new Usuario("Antigo", "antigo@email.com", "123", "000");
        UsuarioDTO dadosNovos = new UsuarioDTO("Novo Nome", "novo@email.com", "999", "000");

        when(repository.findById(id)).thenReturn(Optional.of(usuarioAntigo));
        when(repository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario atualizado = service.atualizarUsuario(id, dadosNovos);

        assertEquals("Novo Nome", atualizado.getNome());
        assertEquals("novo@email.com", atualizado.getEmail());
    }

    // --- TESTES DE DELEÇÃO (DELETE) ---

    @Test
    @DisplayName("Deve deletar usuário existente")
    void deveDeletarUsuario() {
        UUID id = UUID.randomUUID();
        Usuario usuarioMock = new Usuario();

        when(repository.findById(id)).thenReturn(Optional.of(usuarioMock));

        service.deletarUsuario(id);

        verify(repository, times(1)).delete(usuarioMock);
    }

    // --- TESTE DE UPLOAD (EXCEL) ---

    @Test
    @DisplayName("Deve importar usuários via Excel")
    void deveImportarExcel() throws Exception {
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var bos = new java.io.ByteArrayOutputStream()) {

            var sheet = workbook.createSheet("Usuarios");
            sheet.createRow(0).createCell(0).setCellValue("Nome"); // Header

            var row = sheet.createRow(1);
            row.createCell(0).setCellValue("User Excel");
            row.createCell(1).setCellValue("excel@teste.com");
            row.createCell(2).setCellValue("123");
            row.createCell(3).setCellValue("999.999.999-99");

            workbook.write(bos);

            var arquivo = new org.springframework.mock.web.MockMultipartFile(
                    "file", "teste.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray()
            );

            when(repository.existsByEmail(any())).thenReturn(false);
            when(repository.existsByCpf(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("HASH");

            when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
                Usuario u = invocation.getArgument(0);
                ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
                return u;
            });

            service.salvarUsuariosViaExcel(arquivo);

            verify(repository, times(1)).save(any());
            verify(mockSaldoClient, times(1)).criarConta(anyString());
        }
    }
}