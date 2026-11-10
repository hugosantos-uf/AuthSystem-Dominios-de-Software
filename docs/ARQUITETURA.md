# Documento de Arquitetura — AuthSystem

| Item | Valor |
|---|---|
| Projeto | AuthSystem — sistema simples de autenticação |
| Disciplina | Domínios de Software |
| Versão | 1.1 |
| Escopo atual | Backend + Banco de dados + Frontend |

---

## 1. Introdução

### 1.1 Objetivo
Definir a arquitetura e o desenho detalhado de componentes de uma aplicação Web de autenticação, composta por **backend (API REST)**, **banco de dados** e **frontend**. O detalhamento deste documento concentra-se no backend e no banco; o frontend está descrito em [FRONTEND.md](FRONTEND.md). A conformidade com os processos da ISO/IEC 12207 está em [QUALIDADE.md](QUALIDADE.md).

### 1.2 Escopo
O sistema expõe **três endpoints**:

| Funcionalidade | Entrada |
|---|---|
| Cadastro | nome, e-mail, senha, confirmação de senha |
| Login | e-mail, senha |
| Validação do token (`/me`) | cabeçalho `Authorization: Bearer <token>` |

O `/me` não verifica credenciais novas: confere o token emitido no login e devolve a identificação do dono (seção 5.5).

### 1.3 Decisões de tecnologia

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Linguagem | Java 25 (LTS) | Requisito do projeto |
| Framework | Spring Boot 3.x | Padrão de mercado, integração nativa com Spring Security |
| Segurança | Spring Security 6 | Requisito do projeto; BCrypt, filtros, tratamento de erros de segurança |
| Persistência | Spring Data JPA (Hibernate) | Reduz código repetitivo de acesso a dados |
| Banco | PostgreSQL 16 em Docker | Requisito do projeto |
| Migrações | Flyway | Versionamento do esquema, evita `ddl-auto=update` |
| Validação | Jakarta Bean Validation | Validação declarativa nos DTOs |
| Token | JWT (biblioteca `jjwt`) | Login stateless; ver ADR-02 |
| Build | Maven | Convencional, simples |
| Testes | JUnit 5, Mockito, Testcontainers | Unitários e integração com Postgres real |
| Documentação da API | springdoc-openapi (Swagger UI) | Contrato navegável, útil para o frontend |
| Frontend | React 19 + TypeScript + Vite | SPA simples, tipagem do contrato da API; ver [FRONTEND.md](FRONTEND.md) |

---

## 2. Requisitos

### 2.1 Requisitos funcionais

| ID | Requisito |
|---|---|
| RF01 | O sistema deve permitir cadastrar um usuário com nome, e-mail, senha e confirmação de senha. |
| RF02 | O sistema deve rejeitar cadastro com e-mail já existente. |
| RF03 | O sistema deve rejeitar cadastro cuja senha e confirmação sejam diferentes. |
| RF04 | O sistema deve autenticar um usuário por e-mail e senha. |
| RF05 | Em login bem-sucedido, o sistema deve devolver um token de acesso e a identificação do usuário (id, nome, e-mail). |
| RF06 | Todas as falhas devem retornar uma resposta de erro padronizada e clara. |
| RF07 | O frontend deve oferecer telas de cadastro e de login que consumam a API. |
| RF08 | Após autenticar, o frontend deve exibir a identificação do usuário e permitir encerrar a sessão. |
| RF09 | O frontend deve exibir as falhas de forma compreensível, por campo quando aplicável. |

### 2.2 Requisitos não funcionais

| ID | Requisito |
|---|---|
| RNF01 | Senhas armazenadas apenas como hash BCrypt; nunca em texto puro, nunca em logs, nunca em respostas. |
| RNF02 | Mensagens de falha de login não devem revelar se o e-mail existe (evita enumeração de usuários). |
| RNF03 | E-mail tratado sem diferenciar maiúsculas/minúsculas e sem espaços nas pontas. |
| RNF04 | API stateless (sem sessão no servidor). |
| RNF05 | Ambiente reproduzível via Docker Compose. |
| RNF06 | Configuração e segredos via variáveis de ambiente. |

### 2.3 Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Nome: obrigatório, 2 a 100 caracteres. |
| RN02 | E-mail: obrigatório, formato válido, até 254 caracteres, único (normalizado em minúsculas). |
| RN03 | Senha: 8 a 72 caracteres (limite do BCrypt), com ao menos 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial. |
| RN04 | `confirmarSenha` deve ser igual a `senha`. |
| RN05 | Login com e-mail inexistente ou senha incorreta retorna o mesmo erro genérico. |

---

## 3. Visão arquitetural

### 3.1 Contexto do sistema

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','fontFamily':'Segoe UI, Arial'}}}%%
flowchart LR
    U[Usuário] --> F["Frontend"]
    F -- "HTTPS / JSON" --> B["Backend<br/>Spring Boot + Spring Security"]
    B -- "JDBC" --> D[("PostgreSQL<br/>Docker")]
    classDef client fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    classDef front fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    classDef backend fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0F172A
    classDef db fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    class U client
    class F front
    class B backend
    class D db
```

### 3.2 Estilo arquitetural
**Arquitetura em camadas** (Controller → Service → Repository), monólito modular, API REST stateless. Escolhida por ser simples, adequada ao tamanho do problema e fácil de testar. As regras de negócio ficam na camada de serviço, isoladas de HTTP e de persistência.

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','clusterBkg':'#F8FAFC','clusterBorder':'#94A3B8','fontFamily':'Segoe UI, Arial'}}}%%
flowchart TB
    subgraph Backend
        direction TB
        SF["Security Filter Chain<br/>(Spring Security)"]
        C["Camada Web<br/>Controllers + DTOs"]
        S["Camada de Serviço<br/>regras de negócio"]
        R["Camada de Persistência<br/>Repositories + Entities"]
        EH["Tratamento de exceções<br/>@RestControllerAdvice"]
        SF --> C --> S --> R
        EH -.captura erros de.-> C
        EH -.captura erros de.-> S
    end
    R --> DB[("PostgreSQL")]
    classDef sec fill:#FEE2E2,stroke:#DC2626,stroke-width:2px,color:#0F172A
    classDef web fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    classDef svc fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0F172A
    classDef repo fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    classDef err fill:#FFEDD5,stroke:#EA580C,stroke-width:2px,color:#0F172A
    classDef db fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    class SF sec
    class C web
    class S svc
    class R repo
    class EH err
    class DB db
```

### 3.3 Implantação (Docker)

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','clusterBkg':'#F8FAFC','clusterBorder':'#94A3B8','fontFamily':'Segoe UI, Arial'}}}%%
flowchart LR
    subgraph Host["Máquina do desenvolvedor"]
        subgraph Compose["docker compose"]
            PG["postgres:16<br/>porta 5432<br/>volume: pgdata"]
            APP["auth-api (opcional)<br/>porta 8080"]
        end
        DEV["API local via IDE/Maven<br/>porta 8080"]
    end
    APP -- "rede interna" --> PG
    DEV -- "localhost:5432" --> PG
    classDef db fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    classDef app fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0F172A
    classDef dev fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    class PG db
    class APP app
    class DEV dev
    style Compose fill:#E0F2FE,stroke:#0284C7,stroke-width:2px
    style Host fill:#F8FAFC,stroke:#94A3B8
```

O Compose sobe o PostgreSQL com volume persistente. A API pode rodar pela IDE durante o desenvolvimento ou como segundo serviço do Compose (via `Dockerfile` multi-stage) para demonstração.

---

## 4. Desenho detalhado de componentes

### 4.1 Estrutura de pacotes

```
com.faculdade.authsystem
├── AuthSystemApplication.java
├── config/
│   ├── SecurityConfig.java          # SecurityFilterChain, PasswordEncoder
│   ├── JwtProperties.java           # @ConfigurationProperties (segredo, expiração)
│   └── OpenApiConfig.java
├── controller/
│   └── AuthController.java          # POST /api/auth/register, /api/auth/login
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   └── LoginRequest.java
│   └── response/
│   │   ├── RegisterResponse.java
│   │   ├── LoginResponse.java
│   │   └── ErrorResponse.java
├── service/
│   ├── AuthService.java             # interface
│   ├── AuthServiceImpl.java
│   └── JwtService.java              # geração do token
├── repository/
│   └── UserRepository.java
├── domain/
│   └── User.java                    # entidade JPA
├── validation/
│   ├── PasswordsMatch.java          # anotação de classe
│   ├── PasswordsMatchValidator.java
│   └── StrongPassword.java (+ validator)
├── exception/
│   ├── ApiException.java            # base abstrata
│   ├── EmailAlreadyExistsException.java
│   ├── InvalidCredentialsException.java
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice
│   └── ErrorCode.java               # enum
└── security/
    ├── UserDetailsServiceImpl.java         # carrega o usuário por e-mail
    ├── RestAuthenticationEntryPoint.java   # 401 em JSON
    └── RestAccessDeniedHandler.java        # 403 em JSON
```

### 4.2 Diagrama de classes

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','fontFamily':'Segoe UI, Arial'}}}%%
classDiagram
    class AuthController {
        +register(RegisterRequest) ResponseEntity~RegisterResponse~
        +login(LoginRequest) ResponseEntity~LoginResponse~
    }
    class AuthService {
        <<interface>>
        +register(RegisterRequest) RegisterResponse
        +login(LoginRequest) LoginResponse
    }
    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -AuthenticationManager authenticationManager
        -JwtService jwtService
    }
    class JwtService {
        +generateToken(User) String
        +getExpirationSeconds() long
    }
    class UserRepository {
        <<interface>>
        +findByEmail(String) Optional~User~
        +existsByEmail(String) boolean
    }
    class User {
        -UUID id
        -String name
        -String email
        -String passwordHash
        -Instant createdAt
    }
    class GlobalExceptionHandler {
        +handleValidation()
        +handleApiException()
        +handleBadCredentials()
        +handleUnexpected()
    }
    AuthController --> AuthService
    AuthService <|.. AuthServiceImpl
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> JwtService
    UserRepository --> User
    style AuthController fill:#DBEAFE,stroke:#2563EB,stroke-width:2px,color:#0F172A
    style AuthService fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0F172A
    style AuthServiceImpl fill:#DCFCE7,stroke:#16A34A,stroke-width:2px,color:#0F172A
    style JwtService fill:#CFFAFE,stroke:#0891B2,stroke-width:2px,color:#0F172A
    style UserRepository fill:#EDE9FE,stroke:#7C3AED,stroke-width:2px,color:#0F172A
    style User fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    style GlobalExceptionHandler fill:#FFEDD5,stroke:#EA580C,stroke-width:2px,color:#0F172A
```

### 4.3 Responsabilidade de cada componente

| Componente | Responsabilidade | Não deve |
|---|---|---|
| `AuthController` | Receber HTTP, acionar validação (`@Valid`), delegar ao serviço, montar status HTTP | Conter regra de negócio ou acessar repositório |
| DTOs de request | Representar e validar a entrada | Expor entidade JPA |
| DTOs de response | Representar a saída | Incluir senha/hash |
| `AuthServiceImpl` | Normalizar e-mail, checar duplicidade, gerar hash, persistir, autenticar, emitir token | Conhecer detalhes HTTP |
| `JwtService` | Assinar o token (HS256) com `sub`, `iat`, `exp` | Persistir ou validar usuário |
| `UserRepository` | Acesso a dados do usuário | Regra de negócio |
| `SecurityConfig` | Definir rotas públicas, `BCryptPasswordEncoder`, política stateless, CORS | — |
| `GlobalExceptionHandler` | Converter qualquer exceção em `ErrorResponse` | Vazar stack trace ao cliente |

### 4.4 Autenticação no login
O login usa o mecanismo do próprio Spring Security: `AuthenticationManager` + `DaoAuthenticationProvider` + um `UserDetailsService` que carrega o `User` por e-mail e compara a senha com BCrypt. Falhas (`BadCredentialsException`, `UsernameNotFoundException`) são mapeadas para a **mesma** resposta `INVALID_CREDENTIALS` (RN05).

---

## 5. Contrato da API

Prefixo base: `/api/auth`. Formato: `application/json`.

### 5.1 `POST /api/auth/register` — Cadastro

**Request**
```json
{
  "nome": "Maria Silva",
  "email": "maria@exemplo.com",
  "senha": "Senha@123",
  "confirmarSenha": "Senha@123"
}
```

**Resposta 201 Created**
```json
{
  "id": "3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11",
  "nome": "Maria Silva",
  "email": "maria@exemplo.com",
  "criadoEm": "2026-10-30T14:00:00Z"
}
```

| Status | Código de erro | Quando |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Campo ausente/inválido, senha fraca, senhas diferentes |
| 400 | `MALFORMED_REQUEST` | JSON inválido ou corpo ausente |
| 409 | `EMAIL_ALREADY_EXISTS` | E-mail já cadastrado |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Content-Type diferente de JSON |
| 500 | `INTERNAL_ERROR` | Falha inesperada |

### 5.2 `POST /api/auth/login` — Login

**Request**
```json
{ "email": "maria@exemplo.com", "senha": "Senha@123" }
```

**Resposta 200 OK**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "usuario": {
    "id": "3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11",
    "nome": "Maria Silva",
    "email": "maria@exemplo.com"
  }
}
```

`usuario` é a identificação do usuário autenticado exigida pelo enunciado ("retorno de um objeto contendo a identificação do usuário"). Nunca inclui senha ou hash.

| Status | Código de erro | Quando |
|---|---|---|
| 400 | `VALIDATION_ERROR` | E-mail/senha ausentes ou e-mail malformado |
| 401 | `INVALID_CREDENTIALS` | E-mail inexistente **ou** senha incorreta |
| 500 | `INTERNAL_ERROR` | Falha inesperada |

### 5.3 Sequência — Cadastro

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','actorBkg':'#DBEAFE','actorBorder':'#2563EB','actorTextColor':'#0F172A','signalColor':'#334155','signalTextColor':'#0F172A','noteBkgColor':'#FEF3C7','fontFamily':'Segoe UI, Arial'}}}%%
sequenceDiagram
    actor C as Cliente
    participant Ctl as AuthController
    participant Svc as AuthService
    participant Repo as UserRepository
    participant DB as PostgreSQL
    C->>Ctl: POST /register
    Ctl->>Ctl: @Valid (campos + senhas iguais)
    rect rgb(254, 226, 226)
    alt validação falhou
        Ctl-->>C: 400 VALIDATION_ERROR
    end
    end
    Ctl->>Svc: register(request)
    Svc->>Svc: normaliza e-mail
    Svc->>Repo: existsByEmail
    Repo->>DB: SELECT
    rect rgb(255, 237, 213)
    alt já existe
        Svc-->>C: 409 EMAIL_ALREADY_EXISTS
    end
    end
    rect rgb(220, 252, 231)
    Svc->>Svc: BCrypt(senha)
    Svc->>Repo: save(user)
    Repo->>DB: INSERT
    Svc-->>Ctl: RegisterResponse
    Ctl-->>C: 201 Created
    end
```

Observação: a checagem `existsByEmail` não basta em concorrência. A **constraint UNIQUE** no banco é a garantia real; uma `DataIntegrityViolationException` no `save` também é convertida em `EMAIL_ALREADY_EXISTS`.

### 5.4 Sequência — Login

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','actorBkg':'#DBEAFE','actorBorder':'#2563EB','actorTextColor':'#0F172A','signalColor':'#334155','signalTextColor':'#0F172A','noteBkgColor':'#FEF3C7','fontFamily':'Segoe UI, Arial'}}}%%
sequenceDiagram
    actor C as Cliente
    participant Ctl as AuthController
    participant Svc as AuthService
    participant AM as AuthenticationManager
    participant DB as PostgreSQL
    participant J as JwtService
    C->>Ctl: POST /login
    Ctl->>Ctl: @Valid
    Ctl->>Svc: login(request)
    Svc->>AM: authenticate(email, senha)
    AM->>DB: busca usuário por e-mail
    AM->>AM: BCrypt.matches
    rect rgb(254, 226, 226)
    alt falha
        AM-->>Svc: BadCredentialsException
        Svc-->>C: 401 INVALID_CREDENTIALS
    end
    end
    rect rgb(220, 252, 231)
    Svc->>J: generateToken(user)
    J-->>Svc: JWT
    Svc-->>Ctl: LoginResponse
    Ctl-->>C: 200 OK
    end
```

---

### 5.5 `GET /api/auth/me` — Validação do token

**Request**
```
GET /api/auth/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Resposta 200 OK** — mesma identificação devolvida no login:
```json
{ "id": "3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11", "nome": "Maria Silva", "email": "maria@exemplo.com" }
```

| Status | Código de erro | Quando |
|---|---|---|
| 401 | `UNAUTHORIZED` | Token ausente, sem o prefixo `Bearer`, adulterado, assinado com outro segredo, sem assinatura, expirado, ou de usuário que não existe mais |

Como funciona (sem estado no servidor):
1. O `JwtAuthenticationFilter`, na cadeia do Spring Security, lê o cabeçalho `Authorization`.
2. O `JwtService.validate` recalcula a assinatura HS256 com o `JWT_SECRET` e confere o `exp`. Nenhuma consulta a sessão.
3. Token válido → o `sub` (UUID do usuário) vira o principal da requisição. Token inválido → a requisição segue sem autenticação e o `RestAuthenticationEntryPoint` responde 401.
4. O `AuthController.me` recebe o UUID (`@AuthenticationPrincipal`) e o `AuthService.currentUser` busca o usuário para montar a identificação.

Token inválido em rota pública (cadastro, login) é ignorado: não impede o acesso.

## 6. Modelo de dados

```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryColor':'#FEF3C7','primaryBorderColor':'#D97706','primaryTextColor':'#0F172A','lineColor':'#475569','attributeBackgroundColorOdd':'#FFFBEB','attributeBackgroundColorEven':'#FEF3C7','fontFamily':'Segoe UI, Arial'}}}%%
erDiagram
    USERS {
        uuid id PK
        varchar name "NOT NULL, 2..100"
        varchar email UK "NOT NULL, lowercase"
        varchar password_hash "NOT NULL, BCrypt"
        timestamptz created_at "NOT NULL, default now()"
    }
```

Migração inicial `V1__create_users_table.sql`:

```sql
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);
```

- `confirmarSenha` **não é persistido**: existe só no DTO de entrada.
- A constraint UNIQUE já cria o índice usado por `findByEmail`.

---

## 7. Tratamento de exceções

### 7.1 Princípios
1. Um único ponto de conversão: `GlobalExceptionHandler` (`@RestControllerAdvice`).
2. Toda resposta de erro segue o mesmo formato (`ErrorResponse`).
3. Exceções de negócio são tipadas e carregam um `ErrorCode` estável (o frontend decide por código, não por texto).
4. Erros inesperados são logados no servidor com um `traceId` e devolvidos ao cliente de forma genérica.
5. Erros gerados pelo Spring Security (401/403 antes de chegar ao controller) também saem em JSON, via `AuthenticationEntryPoint` e `AccessDeniedHandler`.

### 7.2 Formato padrão
```json
{
  "timestamp": "2026-10-30T14:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Dados inválidos. Corrija os campos indicados.",
  "path": "/api/auth/register",
  "traceId": "a1b2c3d4",
  "errors": [
    { "field": "senha", "message": "A senha deve ter ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo." },
    { "field": "confirmarSenha", "message": "As senhas não coincidem." }
  ]
}
```
`errors` aparece apenas em `VALIDATION_ERROR`.

### 7.3 Mapeamento exceção → resposta

| Exceção | HTTP | `code` | Mensagem ao cliente |
|---|---|---|---|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` | Dados inválidos + lista por campo |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST` | Corpo da requisição ausente ou JSON inválido |
| `HttpMediaTypeNotSupportedException` | 415 | `UNSUPPORTED_MEDIA_TYPE` | Use `application/json` |
| `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` | Método não suportado |
| `NoResourceFoundException` | 404 | `NOT_FOUND` | Recurso não encontrado |
| `EmailAlreadyExistsException` / `DataIntegrityViolationException` (uk_users_email) | 409 | `EMAIL_ALREADY_EXISTS` | E-mail já cadastrado |
| `InvalidCredentialsException` / `BadCredentialsException` / `UsernameNotFoundException` | 401 | `INVALID_CREDENTIALS` | E-mail ou senha inválidos |
| `AuthenticationException` (fora do login) | 401 | `UNAUTHORIZED` | Autenticação necessária |
| `AccessDeniedException` | 403 | `FORBIDDEN` | Acesso negado |
| `Exception` (fallback) | 500 | `INTERNAL_ERROR` | Erro interno. Informe o `traceId` ao suporte. |

### 7.4 Hierarquia
```mermaid
%%{init: {'theme':'base','themeVariables':{'primaryTextColor':'#0F172A','lineColor':'#475569','fontFamily':'Segoe UI, Arial'}}}%%
classDiagram
    RuntimeException <|-- ApiException
    ApiException <|-- EmailAlreadyExistsException
    ApiException <|-- InvalidCredentialsException
    class ApiException {
        <<abstract>>
        -ErrorCode code
        -HttpStatus status
    }
    style RuntimeException fill:#F1F5F9,stroke:#64748B,stroke-width:2px,color:#0F172A
    style ApiException fill:#FFEDD5,stroke:#EA580C,stroke-width:2px,color:#0F172A
    style EmailAlreadyExistsException fill:#FEF3C7,stroke:#D97706,stroke-width:2px,color:#0F172A
    style InvalidCredentialsException fill:#FEE2E2,stroke:#DC2626,stroke-width:2px,color:#0F172A
```

---

## 8. Segurança

| Tema | Decisão |
|---|---|
| Hash de senha | `BCryptPasswordEncoder` (strength 10–12) |
| Rotas | `/api/auth/register`, `/api/auth/login`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` públicas; qualquer outra (incluindo `/api/auth/me`) exige `Authorization: Bearer` com JWT válido |
| Validação do token | `JwtAuthenticationFilter` confere assinatura e `exp` a cada requisição; token sem assinatura (`alg: none`) é recusado |
| Sessão | `SessionCreationPolicy.STATELESS` |
| CSRF | Desabilitado (API stateless, sem cookies de sessão) |
| Token | JWT HS256; segredo com no mínimo 256 bits vindo de variável de ambiente; expiração padrão 1 h |
| CORS | Origem do frontend configurável por propriedade; padrão restritivo |
| Enumeração de usuários | Erro de login único (RN05) |
| Logs | Nunca registrar senha, hash ou token |
| Segredos | `DB_PASSWORD`, `JWT_SECRET` fora do repositório (`.env` no `.gitignore`; `.env.example` versionado) |
| Cabeçalhos | Padrões do Spring Security (X-Content-Type-Options, X-Frame-Options, etc.) |
| Dependências | Verificação periódica de vulnerabilidades |

**Limitação conhecida:** sem rate limiting, o login é vulnerável a força bruta. Sugerido como evolução (seção 11).

---

## 9. Configuração e infraestrutura

### 9.1 Variáveis de ambiente

| Variável | Exemplo | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/authdb` | Conexão |
| `DB_USER` | `auth_user` | Usuário do banco |
| `DB_PASSWORD` | *(definir localmente)* | Senha do banco |
| `JWT_SECRET` | *(string aleatória ≥ 32 bytes)* | Assinatura do token |
| `JWT_EXPIRATION_SECONDS` | `3600` | Validade do token |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | CORS |

### 9.2 `docker-compose.yml` (previsto)

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: authsystem-db
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-authdb}
      POSTGRES_USER: ${POSTGRES_USER:-auth_user}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-auth_user} -d ${POSTGRES_DB:-authdb}"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  pgdata:
```

### 9.3 Execução local
A aplicação roda apenas localmente, com uma única configuração (sem perfis de ambiente). Swagger fica sempre habilitado e os testes de integração sobem um PostgreSQL próprio via Testcontainers.

`spring.jpa.hibernate.ddl-auto=validate`: o esquema só muda por migração Flyway.

---

## 10. Estratégia de testes

| Nível | Alvo | Ferramentas |
|---|---|---|
| Unitário | `AuthServiceImpl` (e-mail duplicado, hash gerado, credenciais inválidas), validadores de senha | JUnit 5, Mockito |
| Slice web | `AuthController` + `GlobalExceptionHandler`: status e corpo de cada erro | `@WebMvcTest`, MockMvc |
| Integração | Fluxo completo cadastro → login contra Postgres real, UNIQUE em concorrência | `@SpringBootTest`, Testcontainers |
| Frontend | Validações, cliente HTTP, sessão e fluxos de tela (API simulada) | Vitest, Testing Library |

Casos mínimos: cadastro válido; senha ≠ confirmação; senha fraca; e-mail inválido; e-mail duplicado (inclusive variando maiúsculas); login válido; senha errada; e-mail inexistente (resposta idêntica à senha errada); JSON malformado; campos nulos.

---

## 11. Decisões arquiteturais (ADRs resumidas)

| ID | Decisão | Alternativas | Motivo |
|---|---|---|---|
| ADR-01 | Arquitetura em camadas | Hexagonal, microsserviços | Escopo pequeno; menor custo cognitivo |
| ADR-02 | Login devolve JWT (stateless) | Sessão com cookie | Facilita frontend desacoplado e futuras APIs; sem estado no servidor |
| ADR-03 | Flyway para esquema | `ddl-auto=update` | Esquema versionado e auditável |
| ADR-04 | UUID como chave primária | Serial/bigint | Não expõe sequência de cadastros; gerado na aplicação |
| ADR-05 | Campos de entrada em português (`nome`, `senha`, `confirmarSenha`) | Inglês | Alinhado ao enunciado; códigos de erro permanecem em inglês por serem identificadores técnicos |
| ADR-06 | Erros com `code` estável + `message` legível | Apenas mensagem | Frontend trata por código; mensagem pode mudar/internacionalizar |
| ADR-07 | Cadastro **não** loga o usuário automaticamente | Retornar token no cadastro | Mantém os dois endpoints com responsabilidade única |
| ADR-08 | Login devolve também `usuario` (id, nome, e-mail) | Cliente decodificar o JWT | Atende ao enunciado; o cliente não depende do formato interno do token |
| ADR-09 | Frontend SPA em React + TypeScript, sem biblioteca de rotas | Angular, Vue, React Router | Três telas apenas; tipagem estática do contrato; menos dependências |
| ADR-10 | Sessão do frontend em `sessionStorage` | `localStorage`, cookie | Some ao fechar a aba; sem backend de sessão (RNF04) |
| ADR-11 | Token validado por filtro do Spring Security; `GET /api/auth/me` devolve o dono do token | Validar o token dentro de cada controller; lista de sessões no servidor | Segurança em ponto único; continua sem estado (RNF04) |

### Evoluções possíveis
Rate limiting/bloqueio por tentativas, refresh token, revogação de tokens antes do `exp`, verificação de e-mail, recuperação de senha, papéis (RBAC), observabilidade (Actuator, métricas), pipeline CI.

---

## 12. Plano de implementação sugerido

1. Estrutura Maven + Spring Boot, `docker-compose.yml`, `.env.example`, `.gitignore`.
2. Migração Flyway, entidade `User`, `UserRepository`.
3. DTOs e validações (incluindo `@PasswordsMatch`, `@StrongPassword`).
4. Exceções, `ErrorCode`, `GlobalExceptionHandler`.
5. `SecurityConfig`, `UserDetailsService`, `JwtService`.
6. `AuthService` + `AuthController`.
7. Testes unitários, slice e integração.
8. Swagger e README de execução.

---

## 13. Glossário

| Termo | Significado |
|---|---|
| JWT | JSON Web Token: token assinado com identidade e expiração |
| BCrypt | Função de hash adaptativa para senhas |
| DTO | Objeto de transferência de dados, separado da entidade |
| ADR | Registro de decisão arquitetural |
| Stateless | O servidor não guarda estado de sessão entre requisições |
