package com.gabriel.desafio.beca.api.domain.user;

import com.gabriel.desafio.beca.api.infra.client.MockSaldoClient;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private MockSaldoClient mockSaldoClient;

    @Transactional
    public Usuario criarUsuario(UsuarioDTO dados) {
        if (repository.existsByEmail(dados.email())) {
            throw new RuntimeException("Email já cadastrado");
        }
        if (repository.existsByCpf(dados.cpf())) {
            throw new RuntimeException("CPF já cadastrado");
        }

        String senhaCriptografada = passwordEncoder.encode(dados.senha());

        Usuario novoUsuario = new Usuario(
                dados.nome(),
                dados.email(),
                senhaCriptografada,
                dados.cpf()
        );

        Usuario usuarioSalvo = repository.save(novoUsuario);

        mockSaldoClient.criarConta(usuarioSalvo.getId().toString());

        return usuarioSalvo;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
    }

    @Transactional
    public Usuario atualizarUsuario(UUID id, UsuarioDTO dados) {
        Usuario usuario = buscarPorId(id);

        usuario.setNome(dados.nome());
        usuario.setEmail(dados.email());
        usuario.setSenha(dados.senha());

        return repository.save(usuario);
    }

    @Transactional
    public void deletarUsuario(UUID id) {
        Usuario usuario = buscarPorId(id);
        repository.delete(usuario);
    }

    @Transactional
    public void salvarUsuariosViaExcel(org.springframework.web.multipart.MultipartFile arquivo) {
        try {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(arquivo.getInputStream());
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);

            org.apache.poi.ss.usermodel.DataFormatter dataFormatter = new org.apache.poi.ss.usermodel.DataFormatter();

            System.out.println("Iniciando leitura do Excel...");

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                if (row.getCell(0) == null && row.getCell(1) == null) {
                    continue;
                }

                try {
                    String nome = dataFormatter.formatCellValue(row.getCell(0));
                    String email = dataFormatter.formatCellValue(row.getCell(1));
                    String senha = dataFormatter.formatCellValue(row.getCell(2));
                    String cpf = dataFormatter.formatCellValue(row.getCell(3));

                    if (nome.isEmpty() || email.isEmpty() || cpf.isEmpty()) {
                        System.out.println("Linha " + row.getRowNum() + " ignorada: dados incompletos.");
                        continue;
                    }

                    UsuarioDTO dto = new UsuarioDTO(nome, email, senha, cpf);
                    criarUsuario(dto);
                    System.out.println("Usuário criado: " + nome);

                } catch (Exception e) {
                    System.err.println("Erro na linha " + row.getRowNum() + ": " + e.getMessage());
                }
            }
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar arquivo: " + e.getMessage());
        }
    }
}