# AuthSystem

Sistema simples de autenticação (cadastro e login) com Java 21, Spring Boot 3, Spring Security e PostgreSQL em Docker.
A arquitetura completa está em [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Pré-requisitos
- JDK 21
- Maven 3.9+
- Docker (para o PostgreSQL e para os testes de integração)

## Como rodar

1. Crie o arquivo de ambiente e preencha os valores:
   ```bash
   cp .env.example .env
   ```
   Defina ao menos `POSTGRES_PASSWORD`, `DB_PASSWORD` (a mesma senha) e `JWT_SECRET` (mínimo 32 caracteres).

2. Suba o banco:
   ```bash
   docker compose up -d
   ```

3. Suba a API (o `.env` da raiz é lido automaticamente):
   ```bash
   cd backend
   mvn spring-boot:run
   ```

A API fica em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui.html`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cadastro (`nome`, `email`, `senha`, `confirmarSenha`) |
| POST | `/api/auth/login` | Login (`email`, `senha`), devolve o token JWT |

```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@exemplo.com","senha":"Senha@123","confirmarSenha":"Senha@123"}'

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"maria@exemplo.com","senha":"Senha@123"}'
```

Todos os erros seguem o formato `{ timestamp, status, code, message, path, traceId, errors? }`. Veja a seção 7 do documento de arquitetura.

## Testes

```bash
cd backend
mvn test
```

Os testes de integração usam Testcontainers e precisam do Docker em execução; sem ele, são ignorados.

## API em container (opcional)

```bash
docker build -t authsystem-api backend
docker run --rm -p 8080:8080 --env-file .env \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/authdb authsystem-api
```
