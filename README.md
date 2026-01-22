# 🏦 Desafio Beca - Sistema Bancário Resiliente

API REST desenvolvida para simular operações bancárias com arquitetura de microsserviços, mensageria assíncrona e relatórios.

## 🚀 Tecnologias Utilizadas

- **Java 17** + **Spring Boot 3**
- **Spring Security** + **JWT** (Autenticação Stateless)
- **Spring Data JPA** (PostgreSQL / H2)
- **Apache Kafka** (Mensageria e Processamento Assíncrono)
- **OpenFeign** (Integração com BrasilAPI e MockAPI)
- **OpenPDF** (Geração de Extratos Bancários)
- **Swagger/OpenAPI** (Documentação Automática)
- **JUnit 5 + Mockito** (Testes Unitários)
- **Docker** (Containerização do Kafka e Zookeeper)

## ⚙️ Funcionalidades

### 1. Transações Financeiras
- **Depósito:** Adiciona saldo e notifica via Kafka.
- **Saque/Transferência:** Valida saldo na API externa (Mock), debita e notifica via Kafka.
- **Câmbio:** Consulta cotação do Dólar em tempo real (BrasilAPI) e grava na transação.

### 2. Processamento Assíncrono (CQRS Lite)
- Toda transação gera um evento no tópico `transacoes-realizadas`.
- Um **Consumer** escuta esses eventos e atualiza uma tabela de Analytics (Total gasto no dia) sem travar a API principal.

### 3. Relatórios e Documentação
- **PDF:** Endpoint `/transacoes/exportar` gera um extrato detalhado.
- **Swagger:** Interface interativa em `/swagger-ui.html`.

## 🛠️ Como Rodar

1. **Subir o Kafka (Docker):**
   ```bash
   docker-compose up -d

2. **Rodar a Aplicação:**
* Execute a classe `DesafioBecaApiApplication`.
* A API rodará em `http://localhost:8080`.


3. **Acessar Documentação:**
* Abra `http://localhost:8080/swagger-ui.html`



## 🧪 Testes

O projeto conta com testes unitários cobrindo o Core Business (`TransacaoService`), validando:

* Cálculos de saldo.
* Integração com Mocks.
* Disparo de eventos Kafka.

---

Desenvolvido por **Gabriel Reis** 🚀
