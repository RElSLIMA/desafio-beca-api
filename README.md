# 🏦 Desafio Beca - Sistema Bancário

API REST robusta desenvolvida para simular operações bancárias reais.  
O projeto utiliza **Arquitetura Orientada a Eventos** com **Apache Kafka** para garantir **alta disponibilidade**, **resiliência** e **processamento assíncrono** de transações.

---

## 🚀 Tecnologias e Arquitetura

- **Java 17**
- **Spring Boot 3**
- **Apache Kafka** (Mensageria e Processamento Assíncrono)
- **Docker & Docker Compose** (Containerização completa)
- **PostgreSQL** (Banco de Dados Relacional)
- **JUnit 5 & Mockito** (Testes Unitários)
- **Swagger / OpenAPI** (Documentação Interativa)
- **OpenFeign** (Comunicação com APIs Externas)

---

## ⚙️ Pré-requisitos

Para rodar este projeto, você precisa apenas de:

- **Docker**
- **Git**

> 🚫 Não é necessário instalar Java, Maven ou PostgreSQL localmente.  
> Todo o ambiente é provisionado automaticamente via Docker.

---

## 🛠️ Como Rodar (Zero Config)

### 1️⃣ Clone o repositório

```bash
git clone https://github.com/SEU-USER/desafio-beca-api.git
cd desafio-beca-api
```

### 2️⃣ Suba a aplicação 🐳

Execute o comando abaixo.  
O Docker irá **baixar dependências**, **compilar o projeto**, **subir o banco**, **configurar o Kafka/Zookeeper** e iniciar a API.

```bash
docker-compose up -d --build
```

Aguarde até que todos os containers estejam com status **Up**.

### 3️⃣ Acompanhe o processamento (Opcional)

Para visualizar o Kafka processando as transações em tempo real:

```bash
docker logs -f desafio-beca-api
```

---

### 🔄 Roteiro de Teste (Fluxo Assíncrono)

1. **Crie um Usuário**
    - Endpoint: `POST /usuarios`
    - Copie o `id` gerado.
    - Utilize o mesmo `email` e `senha` para o login.

2. **Realize o Login**
    - Endpoint: `POST /login`
    - Informe:
        - `email`
        - `senha`
    - Retorno esperado: **200 OK**
    - Copie o **token JWT** retornado.

   > 🔐 O token será utilizado para autenticar todas as requisições protegidas.

3. **Crie uma Transação**
    - Endpoint: `POST /transacoes`
    - Headers (Swagger / API Client):
        - `Auth` → `Bearer Token` → `SEU_TOKEN_AQUI`
    - Body:
        - `valor`
        - `tipo: "DEPOSITO"`
        - `usuarioId`
    - Retorno esperado: **200 OK**

4. **Verifique o Resultado**
    - Endpoint: `GET /transacoes/extrato`
    - Headers:
        - `Auth` → `Bearer Token` → `SEU_TOKEN_AQUI`
    - Alternativamente, acompanhe os logs da aplicação:
      ```
      PROCESSOR: Transação APROVADA
      ```

---

## 🛡️ Testes Unitários

O projeto possui cobertura de testes para regras críticas de negócio, incluindo:

- Validação de saldo
- Fluxo de mensageria Kafka
- Resiliência e retentativas
- Dead Letter Queue (DLQ)

Para rodar os testes:

```bash
mvn test
```

---

## 🧠 Destaques da Arquitetura

### 🔹 Processamento Assíncrono
A API não bloqueia o cliente aguardando validações externas.  
As transações são publicadas no tópico Kafka:

```
transaction.requested
```

### 🔹 Resiliência com DLQ
- Tentativas automáticas: **3**
- Em caso de falha definitiva, a mensagem é enviada para:
```
transaction.dead-letter
```

Isso garante que nenhuma transação seja perdida.

### 🔹 Saldos Isolados
Integração com **MockAPI**, garantindo:
- Contas independentes por usuário
- Saldo persistente por identidade

### 🔹 Zero Config
Ambiente padronizado e reproduzível com Docker.

---

## 📂 Acesso ao Banco de Dados (Opcional)

Caso queira inspecionar o PostgreSQL:

- **Host:** localhost
- **Porta:** 5432
- **Banco:** desafio_db
- **Usuário:** postgres
- **Senha:** password

---