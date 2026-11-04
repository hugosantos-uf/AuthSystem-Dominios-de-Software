# AuthSystem

Sistema simples de autenticação (cadastro e login) com Java 25, Spring Boot 3, Spring Security e PostgreSQL em Docker, e frontend em React + TypeScript.

| Documento | Conteúdo |
|---|---|
| [docs/ARQUITETURA.md](docs/ARQUITETURA.md) | Requisitos, arquitetura, contrato da API |
| [docs/FRONTEND.md](docs/FRONTEND.md) | Desenho do frontend |
| [docs/QUALIDADE.md](docs/QUALIDADE.md) | Processos da ISO/IEC 12207, rastreabilidade e verificação |

## Pré-requisitos
- JDK 25
- Maven 3.9+
- Node.js 20+ (frontend)
- Docker (para o PostgreSQL e para os testes de integração)

## Como rodar

Primeiro crie o arquivo de ambiente e preencha os valores:
```bash
cp .env.example .env
```
Defina ao menos `POSTGRES_PASSWORD`, `DB_PASSWORD` (a mesma senha) e `JWT_SECRET` (mínimo 32 caracteres).

### Opção A: tudo em Docker (só precisa do Docker)

```bash
docker compose up -d --build
```

| Serviço | Endereço |
|---|---|
| Frontend | `http://localhost:3000` |
| API | `http://localhost:8080` (Swagger em `/swagger-ui.html`) |
| PostgreSQL | `localhost:5432` |

Para acompanhar os logs: `docker compose logs -f api`. Para parar: `docker compose down` (os dados ficam no volume; `down -v` apaga).
Depois de alterar o código, rode de novo com `--build`.

### Opção B: desenvolvimento local (JDK, Maven e Node)

1. Suba só o banco:
   ```bash
   docker compose up -d postgres
   ```

2. Suba a API (o `.env` da raiz é lido automaticamente):
   ```bash
   cd backend
   mvn spring-boot:run
   ```

A API fica em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui.html`.

3. Suba o frontend, em outro terminal:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

O frontend fica em `http://localhost:3000`, a origem já liberada no CORS da API. Para apontar para outra API, copie `frontend/.env.example` para `frontend/.env` e ajuste `VITE_API_URL`.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/register` | Cadastro (`nome`, `email`, `senha`, `confirmarSenha`) |
| POST | `/api/auth/login` | Login (`email`, `senha`), devolve o token JWT e a identificação do usuário (`usuario`) |
| GET | `/api/auth/me` | Valida o token (`Authorization: Bearer <token>`) e devolve a identificação do usuário; 401 se ausente, inválido ou expirado |

```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","email":"maria@exemplo.com","senha":"Senha@123","confirmarSenha":"Senha@123"}'

curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"maria@exemplo.com","senha":"Senha@123"}'

# use o accessToken devolvido pelo login
curl http://localhost:8080/api/auth/me -H "Authorization: Bearer <accessToken>"
```

Todos os erros seguem o formato `{ timestamp, status, code, message, path, traceId, errors? }`. Veja a seção 7 do documento de arquitetura.

## Testes

```bash
cd backend
mvn test
```

Os testes de integração usam Testcontainers e precisam do Docker em execução; sem ele, são ignorados.

```bash
cd frontend
npm run lint        # análise estática (ESLint)
npm run typecheck   # verificação de tipos (TypeScript)
npm test            # testes (Vitest + Testing Library)
npm run test:coverage
```

O pipeline em [.github/workflows/ci.yml](.github/workflows/ci.yml) roda tudo isso a cada push e pull request.

## Containers

| Serviço | Imagem | Observação |
|---|---|---|
| `postgres` | `postgres:16-alpine` | Volume `pgdata` |
| `api` | `backend/Dockerfile` (Maven → JRE 25, usuário não-root) | Conecta ao banco pelo nome do serviço (`postgres:5432`) |
| `frontend` | `frontend/Dockerfile` (Node → Nginx) | `VITE_API_URL` é fixado no build; padrão `http://localhost:8080` |
