package com.example.distribuidora.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI distribuidoraOpenApi() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Distribuidora API")
                        .version("1.0.0")
                        .description("API REST de gestión operativa para distribuidora con stock exclusivamente por camión, "
                                + "devoluciones históricas, pagos con desglose por medio, comprobantes PDF, historiales "
                                + "de ventas/repartos/clientes y control de acceso por permisos (JWT). "
                                + "Formato estándar de fechas: ISO-8601 (YYYY-MM-DD para LocalDate, YYYY-MM-DDTHH:mm:ss para LocalDateTime)."))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
