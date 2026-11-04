package com.faculdade.authsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo contra um PostgreSQL real. Requer Docker em execução; sem Docker, é ignorado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "jwt.secret=0123456789abcdef0123456789abcdef-test-secret",
        "DB_PASSWORD=unused-testcontainers-provides-the-connection"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private static String register(String nome, String email, String senha, String confirmar) {
        return """
                {"nome":"%s","email":"%s","senha":"%s","confirmarSenha":"%s"}""".formatted(nome, email, senha, confirmar);
    }

    private static String login(String email, String senha) {
        return """
                {"email":"%s","senha":"%s"}""".formatted(email, senha);
    }

    @Test
    void registerThenLoginHappyPath() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("Maria Silva", "fluxo@exemplo.com", "Senha@123", "Senha@123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("fluxo@exemplo.com"));

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email = ?", String.class,
                "fluxo@exemplo.com");
        assertNotEquals("Senha@123", hash);
        assertEquals(true, hash.startsWith("$2"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("fluxo@exemplo.com", "Senha@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.id").exists())
                .andExpect(jsonPath("$.usuario.nome").value("Maria Silva"))
                .andExpect(jsonPath("$.usuario.email").value("fluxo@exemplo.com"));
    }

    @Test
    void duplicateEmailIgnoringCaseReturns409() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("Ana", "dup@exemplo.com", "Senha@123", "Senha@123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("Ana 2", "DUP@Exemplo.com", "Senha@123", "Senha@123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnTheSameResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(register("Bia", "bia@exemplo.com", "Senha@123", "Senha@123")))
                .andExpect(status().isCreated());

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("bia@exemplo.com", "Errada@123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("naoexiste@exemplo.com", "Senha@123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn();

        String a = wrongPassword.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String b = unknownEmail.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(extractMessage(a), extractMessage(b));
    }

    @Test
    void concurrentRegistrationsOfSameEmailCreateExactlyOneUser() throws Exception {
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                                .content(register("Concorrente", "race@exemplo.com", "Senha@123", "Senha@123")))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();

        int created = 0;
        int conflicts = 0;
        for (Future<Integer> result : results) {
            int code = result.get();
            if (code == 201) {
                created++;
            } else if (code == 409) {
                conflicts++;
            }
        }
        pool.shutdown();

        assertEquals(1, created);
        assertEquals(threads - 1, conflicts);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM users WHERE email = 'race@exemplo.com'",
                Integer.class));
    }

    private static String extractMessage(String json) {
        int i = json.indexOf("\"message\":\"");
        return json.substring(i, json.indexOf('"', i + 11));
    }
}
