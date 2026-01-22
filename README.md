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

## 🛠️ Como Rodar (Passo a Passo)

### 1. Clonar o repositório
```bash
    git clone [https://github.com/RElSLIMA/desafio-beca-api.git](https://github.com/RElSLIMA/desafio-beca-api.git)
    cd desafio-beca-api
```

### 2. Gerar o executável (.jar)

Antes de subir o Docker, é necessário compilar o projeto:

```bash
    mvn clean package -DskipTests
```

### 3. Subir a Aplicação com Docker

Este comando subirá o Zookeeper, Kafka e a API Containerizada:

```bash
    docker-compose up -d --build
```

### 4. Acessar

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](https://www.google.com/search?q=http://localhost:8080/swagger-ui.html)
* **API:** http://localhost:8080

---

### 🧪 Testes

Para rodar os testes unitários:

```bash
    mvn test
```

## 🐳 Detalhes da Infraestrutura Docker

O projeto utiliza `host.docker.internal` para conectar o container da API ao PostgreSQL do host (máquina local). Certifique-se de que seu banco local aceite conexões TCP/IP.
