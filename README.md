# 🏦 Desafio Beca - Sistema Bancário 

API REST desenvolvida para simular operações bancárias com arquitetura de microsserviços, mensageria assíncrona e relatórios.

## 🚀 Tecnologias

- **Java 17** + **Spring Boot 3**
- **Docker** & **Docker Compose**
- **Apache Kafka**
- **PostgreSQL**
- **Swagger/OpenAPI**

## ⚙️ Pré-requisitos para Rodar

Para executar este projeto, você precisará de:
1. **Java 17** e **Maven** instalados.
2. **Docker** e **Docker Compose** instalados.
3. **PostgreSQL** rodando localmente na porta `5432`.
   - Crie um banco de dados chamado: `desafio_db`.
   - Usuário/Senha configurados no `application.properties` (padrão `postgres`/`postgres` ou ajuste conforme seu ambiente).

## 🛠️ Como Rodar

Você só precisa ter o **Docker** instalado. Não é necessário Java, Maven ou Postgres local.

### 1. Clone o repositório:
```bash
    git clone [https://github.com/SEU-USER/desafio-beca-api.git](https://github.com/SEU-USER/desafio-beca-api.git)
    cd desafio-beca-api
```

### 2. Rode tudo com um único comando:

```bash
    docker-compose up -d --build
```
(Na primeira vez, vai demorar uns minutos pois o Docker vai baixar o Maven e compilar o projeto para você).

### 3. Acessar

* **Swagger UI (Documentação):** http://localhost:8080/swagger-ui.html
* **Banco de Dados (Postgres):** localhost:5432
    * **User:** postgres
    * **Password:** password
---

### 🧪 Testes

Para rodar os testes unitários:

```bash
    mvn test
```

## 🐳 Detalhes da Infraestrutura Docker

O projeto utiliza `host.docker.internal` para conectar o container da API ao PostgreSQL do host (máquina local). Certifique-se de que seu banco local aceite conexões TCP/IP.
