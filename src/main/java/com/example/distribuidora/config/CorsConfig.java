package com.example.distribuidora.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // En lugar de setAllowedOrigins, usas PATTERNS.
        // "/**" o "*" significa "acepta de cualquier origen"
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // Métodos específicos (se recomienda listarlos explícitamente)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Cualquier header
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Ahora SÍ puedes tener esto en true sin que explote
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}