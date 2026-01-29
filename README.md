# 📊 Desafio Beca - Sistema de Gestão Financeira

API REST desenvolvida para simular **controle de finanças pessoais**, permitindo o gerenciamento de usuários, transações e relatórios financeiros.  
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
git clone https://github.com/RElSLIMA/desafio-beca-api.git
cd desafio-beca-api
```

### 2️⃣ Suba a aplicação 🐳

```bash
docker-compose up -d --build
```

Aguarde até que todos os containers estejam com status **Up**.

### 3️⃣ Acompanhe o processamento (Opcional)

```bash
docker logs -f desafio-beca-api
```

---

## 🔄 Roteiro de Teste (Fluxo Assíncrono)

1. **Crie um Usuário**
    - Endpoint: `POST /usuarios`
    - Copie o `id` gerado.

2. **Realize o Login**
    - Endpoint: `POST /login`
    - Copie o **token JWT** retornado.

3. **Crie uma Transação**
    - Endpoint: `POST /transacoes`
    - Utilize o token no header:
      `Auth → Bearer Token → SEU_TOKEN_AQUI`

4. **Verifique o Resultado**
    - Endpoint: `GET /transacoes/extrato`
    - Ou acompanhe os logs:
      ```
      PROCESSOR: Transação APROVADA
      ```

---

## 🛡️ Testes Unitários

Cobertura de testes para regras críticas:

- Validação de saldo
- Fluxo de mensageria Kafka
- Resiliência e retentativas
- Dead Letter Queue (DLQ)

```bash
mvn test
```

---

## 🧠 Destaques da Arquitetura

### 🔹 Processamento Assíncrono
As transações financeiras são publicadas no tópico Kafka:

```
transaction.requested
```

### 🔹 Resiliência com DLQ
- Tentativas automáticas: **3**
- Em caso de falha definitiva:
```
transaction.dead-letter
```

### 🔹 Saldos Isolados por Usuário
Cada usuário possui seu próprio controle financeiro independente.

### 🔹 Zero Config
Ambiente padronizado e reproduzível com Docker.

---

## 📂 Acesso ao Banco de Dados (Opcional)

- **Host:** localhost
- **Porta:** 5432
- **Banco:** desafio_db
- **Usuário:** postgres
- **Senha:** password

---

# 🧪 MANUAL DE TESTES - API DESAFIO BECA

---

## IMPORTANTE
Para as rotas protegidas, faça o login (Passo 1.2), copie o **token JWT** gerado e cole em:

**Auth → Bearer Token**

---

## 1. BLOCO DE AUTENTICAÇÃO E USUÁRIOS

### 1.1 Criar Usuário (Público)
**POST** `http://localhost:8080/usuarios`
```json
{
  "nome": "Gabriel Chefe",
  "email": "gabriel@email.com",
  "senha": "123",
  "cpf": "11122233344"
}
```

### 1.2 Fazer Login (Público)
**POST** `http://localhost:8080/login`
```json
{
  "email": "gabriel@email.com",
  "senha": "123"
}
```

### 1.3 Listar Usuários (Protegido)
**GET** `http://localhost:8080/usuarios`

### 1.4 Upload de Excel (Protegido)
**POST** `http://localhost:8080/usuarios/upload`  
Multipart → campo **file** (.xlsx)

---

## 2. BLOCO DE TRANSAÇÕES

### 2.1 Registrar Depósito
```json
{
  "valor": 1000.00,
  "tipo": "DEPOSITO",
  "categoria": "OUTROS",
  "usuarioId": "COLE_O_UUID_AQUI",
  "moeda": "BRL"
}
```

### 2.2 Registrar Saque
```json
{
  "valor": 50.00,
  "tipo": "SAQUE",
  "categoria": "ALIMENTACAO",
  "usuarioId": "COLE_O_UUID_AQUI",
  "moeda": "BRL"
}
```

### 2.3 Registrar Transferência
```json
{
  "valor": 100.00,
  "tipo": "TRANSFERENCIA",
  "categoria": "OUTROS",
  "usuarioId": "UUID_REMETENTE",
  "destinatarioId": "UUID_DESTINATARIO",
  "moeda": "BRL"
}
```

### 2.4 Consultar Saldo
`GET http://localhost:8080/transacoes/saldo?usuarioId=UUID`

### 2.5 Consultar Extrato
`GET http://localhost:8080/transacoes/extrato?usuarioId=UUID`

---

## 3. RELATÓRIOS E ANÁLISES

### 3.1 Análise por Período
`GET http://localhost:8080/transacoes/analise?usuarioId=UUID&inicio=2026-01-01&fim=2026-01-31`

### 3.2 Análise por Categoria
`GET http://localhost:8080/transacoes/analise/categoria?usuarioId=UUID&inicio=2026-01-01&fim=2026-01-31`

### 3.3 Exportar PDF
`GET http://localhost:8080/transacoes/exportar?usuarioId=UUID`
