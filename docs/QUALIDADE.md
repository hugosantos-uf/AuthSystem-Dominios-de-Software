# Qualidade de Software — Aderência à ISO/IEC/IEEE 12207

| Item | Valor |
|---|---|
| Projeto | AuthSystem |
| Norma de referência | ISO/IEC/IEEE 12207:2017 — Processos do ciclo de vida de software |
| Versão | 1.0 |

---

## 1. Objetivo
Mostrar como os processos da ISO/IEC 12207 aplicáveis a um projeto acadêmico de pequeno porte foram executados no AuthSystem, e onde estão as evidências (artefatos) de cada um. A norma permite **adaptar (tailoring)** os processos ao tamanho e ao risco do projeto. A seção 2 registra essa adaptação.

## 2. Adaptação (tailoring)

| Grupo de processos | Aplicação no projeto |
|---|---|
| Processos de acordo (aquisição, fornecimento) | **Não aplicável.** Não há contrato; o "cliente" é o enunciado da disciplina. |
| Processos organizacionais habilitadores | Aplicados de forma reduzida: modelo de ciclo de vida e infraestrutura. |
| Processos de gestão técnica | Aplicados: planejamento, decisão, riscos, configuração, informação, medição, garantia da qualidade. |
| Processos técnicos | Aplicados do levantamento de requisitos até a transição. Operação e descarte: **não aplicável** (o sistema não vai para produção). |

**Modelo de ciclo de vida:** incremental. Cada incremento corresponde a um commit temático: arquitetura → estrutura e DTOs → exceções → segurança → testes e documentação da API → frontend e integração.

## 3. Processos e evidências

### 3.1 Processos organizacionais habilitadores

| Processo (12207) | Como foi feito | Evidência |
|---|---|---|
| Gestão do modelo de ciclo de vida (6.2.1) | Ciclo incremental, definido na seção 2 | Este documento; histórico do Git |
| Gestão de infraestrutura (6.2.2) | Ambiente reproduzível: banco em Docker, versões fixadas, pipeline de CI | `docker-compose.yml`, `backend/pom.xml`, `frontend/package-lock.json`, `.github/workflows/ci.yml` |

### 3.2 Processos de gestão técnica

| Processo (12207) | Como foi feito | Evidência |
|---|---|---|
| Planejamento do projeto (6.3.1) | Plano de implementação em etapas | [ARQUITETURA.md](ARQUITETURA.md), seção 12 |
| Gestão de decisões (6.3.3) | Decisões registradas como ADRs, com alternativas e motivo | [ARQUITETURA.md](ARQUITETURA.md), seção 11 (ADR-01 a ADR-10) |
| Gestão de riscos (6.3.4) | Riscos identificados e tratados ou aceitos (seção 6 deste documento) | Seção 6 |
| Gestão de configuração (6.3.5) | Git como repositório de itens de configuração; dependências travadas; esquema do banco versionado; segredos fora do repositório | `.gitignore` (raiz, `backend/`, `frontend/`), `package-lock.json`, migrações Flyway, `.env.example` |
| Gestão da informação (6.3.6) | Documentação versionada junto com o código, em Markdown | `README.md`, `docs/` |
| Medição (6.3.7) | Cobertura de testes medida a cada execução do pipeline | `npm run test:coverage`; seção 5 |
| Garantia da qualidade (6.3.8) | Portões automáticos: lint, verificação de tipos, testes e build precisam passar em todo push e pull request | `.github/workflows/ci.yml`, `frontend/eslint.config.js`, `frontend/tsconfig.json` (`strict`) |

### 3.3 Processos técnicos

| Processo (12207) | Como foi feito | Evidência |
|---|---|---|
| Definição de necessidades dos stakeholders (6.4.2) | Enunciado da disciplina decomposto nos itens E1–E6 (seção 4) | Seção 4 |
| Definição de requisitos do sistema (6.4.3) | Requisitos funcionais, não funcionais e regras de negócio identificados | [ARQUITETURA.md](ARQUITETURA.md), seção 2 (RF01–RF09, RNF01–RNF06, RN01–RN05) |
| Definição da arquitetura (6.4.4) | Camadas, contexto, implantação | [ARQUITETURA.md](ARQUITETURA.md), seção 3 |
| Definição do desenho (6.4.5) | Componentes, contrato da API, modelo de dados, erros | [ARQUITETURA.md](ARQUITETURA.md), seções 4–8; [FRONTEND.md](FRONTEND.md) |
| Implementação (6.4.7) | Código seguindo o desenho, com responsabilidades separadas | `backend/src/main`, `frontend/src` |
| Integração (6.4.8) | Frontend e backend integrados pelo contrato da seção 5 da arquitetura; CORS configurado para a origem do frontend; tipos do contrato em `frontend/src/api/types.ts`. Na integração, o login passou a devolver `usuario`, exigido pelo enunciado (ADR-08). | `frontend/src/api/`, `SecurityConfig`, `LoginResponse` |
| Verificação (6.4.9) | Testes automatizados em três níveis no backend e quatro grupos no frontend; análise estática | Seção 5 |
| Transição (6.4.10) | Instruções de instalação e execução; sistema completo (banco, API e frontend) sobe com um comando | `README.md`, `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` |
| Validação (6.4.11) | Roteiro de aceitação, executado contra o enunciado | Seção 7 |
| Manutenção (6.4.13) | Código tipado e testado; erros com `traceId` para diagnóstico; evoluções listadas | [ARQUITETURA.md](ARQUITETURA.md), seção 11 ("Evoluções possíveis") |

## 4. Matriz de rastreabilidade

Do enunciado ao requisito, e do requisito à implementação e ao teste que o verifica.

| Enunciado | Requisitos | Backend (implementação / teste) | Frontend (implementação / teste) |
|---|---|---|---|
| **E1** Entrada de credenciais (e-mail + senha) | RF04, RN02 | `LoginRequest`, `AuthController.login` / `AuthControllerTest.loginWithBlankFieldsReturnsValidationError` | `LoginPage`, `validateLogin` / CT-V01–V03, CT-F03 |
| **E2** Verificar existência do usuário | RF04, RNF03 | `UserDetailsServiceImpl`, `AuthServiceImpl.login` / `AuthIntegrationTest.wrongPasswordAndUnknownEmailReturnTheSameResponse` | — (decisão do servidor) |
| **E3** Comparar credencial fornecida com a registrada | RF04, RNF01 | `DaoAuthenticationProvider` + `BCryptPasswordEncoder` / `AuthIntegrationTest.registerThenLoginHappyPath` | — (decisão do servidor) |
| **E4** Decisão: autenticado ou não | RF04, RN05, RNF02 | `AuthServiceImpl.login`, `GlobalExceptionHandler` / `AuthServiceImplTest.loginMapsAnyAuthenticationFailureToInvalidCredentials`, `AuthControllerTest.loginWithInvalidCredentialsReturns401` | `LoginPage` / CT-F01, CT-F02 |
| **E5** Sucesso: objeto com a identificação do usuário | RF05, RF08 | `LoginResponse.usuario` / `AuthServiceImplTest.loginReturnsBearerTokenAndUserIdentity`, `AuthControllerTest.loginReturns200WithTokenAndUserIdentity`, `AuthIntegrationTest.registerThenLoginHappyPath` | `HomePage`, `session.ts` / CT-A01, CT-F01, CT-F05, CT-F10, CT-S01–S05 |
| **E6** Falha: erro de autenticação | RF06, RF09 | `ErrorResponse`, `ErrorCode.INVALID_CREDENTIALS` / `AuthIntegrationTest.wrongPasswordAndUnknownEmailReturnTheSameResponse` | `ApiError`, `errorMessages`, `ToastProvider` / CT-A02–A07, CT-F02, CT-F04, CT-T01–T04 |
| Cadastro (pré-condição para haver usuário válido) | RF01–RF03, RN01–RN04 | `RegisterRequest`, validadores, `AuthServiceImpl.register` / `AuthControllerTest.register*`, `StrongPasswordValidatorTest`, `AuthServiceImplTest.register*`, `AuthIntegrationTest.duplicateEmailIgnoringCase*`, `concurrentRegistrations*` | `RegisterPage`, `validateRegister` / CT-V04–V10, CT-F06–F09, CT-F12 |
| Telas de cadastro e login | RF07 | — | `App`, `LoginPage`, `RegisterPage` / CT-F01–F09 |

## 5. Registro de verificação

| Item verificado | Ferramenta | Critério de aceite | Resultado (02/11/2026) |
|---|---|---|---|
| Frontend: análise estática | ESLint | 0 erros | ✅ 0 erros |
| Frontend: tipos | `tsc --noEmit` (strict) | 0 erros | ✅ 0 erros |
| Frontend: testes | Vitest | 100% passando | ✅ 46 de 46 |
| Frontend: cobertura | `@vitest/coverage-v8` | ≥ 80% de linhas | ✅ 99,0% de linhas, 93,4% de ramos |
| Frontend: build de produção | Vite | Build sem erros | ✅ |
| Backend: testes unitários, slice e integração | JUnit 5, Mockito, Testcontainers | 100% passando | ⚠️ Pendente: executar `mvn verify` (JDK e Docker necessários) ou conferir o pipeline de CI |

O pipeline de CI repete todos esses itens a cada alteração, então o registro acima pode ser atualizado a partir da última execução.

## 6. Riscos

| Risco | Impacto | Tratamento |
|---|---|---|
| Força bruta no login (sem rate limiting) | Alto | **Aceito** para o escopo acadêmico; documentado como evolução ([ARQUITETURA.md](ARQUITETURA.md), seção 8) |
| Enumeração de usuários pela mensagem de erro | Médio | **Mitigado**: erro único (RN05), testado no backend e no frontend |
| Vazamento de senha em log ou resposta | Alto | **Mitigado**: `toString` mascarado nos DTOs, apenas hash BCrypt persistido, senha limpa da tela após falha |
| Token roubado por XSS no navegador | Médio | **Reduzido**: React escapa todo conteúdo, `sessionStorage` com expiração. Cookie `HttpOnly` fica como evolução. |
| Divergência entre as regras do cliente e do servidor | Baixo | **Mitigado**: mesmas regras e mensagens; o servidor é a fonte da verdade e seus erros por campo são exibidos (CT-F09) |
| Segredos versionados por engano | Alto | **Mitigado**: `.env` no `.gitignore`, apenas `.env.example` versionado |

## 7. Validação (roteiro de aceitação)

Execução manual com backend, banco e frontend em execução (ver `README.md`). Cada passo valida um item do enunciado.

| # | Passo | Resultado esperado | Enunciado |
|---|---|---|---|
| 1 | Na tela de login, clicar em "Cadastre-se", preencher dados válidos e enviar | Volta ao login com toast "Cadastro realizado" e o e-mail preenchido | Pré-condição |
| 2 | Informar a senha correta e entrar | Tela "Autenticado" mostrando nome, e-mail e identificador do usuário | E1, E2, E3, E4, E5 |
| 3 | Clicar em "Sair" | Volta ao login; recarregar a página não reabre a sessão | E5 |
| 4 | Entrar com senha errada | Toast "E-mail ou senha inválidos."; senha apagada | E4, E6 |
| 5 | Entrar com e-mail não cadastrado | Exatamente a mesma mensagem do passo 4 | E2, E6, RN05 |
| 6 | Enviar o login com campos vazios ou e-mail malformado | Mensagens nos campos; nenhuma requisição enviada | E1 |
| 7 | Cadastrar de novo o mesmo e-mail, variando maiúsculas | Toast "Este e-mail já está cadastrado." | RF02, RNF03 |
| 8 | Parar a API e tentar entrar | Toast "Não foi possível conectar ao servidor…" | RF09 |
