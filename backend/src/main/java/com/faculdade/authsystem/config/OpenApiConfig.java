package com.faculdade.authsystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Nome do esquema usado pelas rotas que exigem o token ({@code Authorization: Bearer}). */
    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI authSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AuthSystem API")
                        .version("v1")
                        .description("API de autenticação: cadastro, login e validação do token."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
