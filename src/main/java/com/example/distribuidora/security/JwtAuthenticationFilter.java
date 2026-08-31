package com.example.distribuidora.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// OncePerRequestFilter: garantiza que este filtro corra UNA sola vez por
// request (evita ejecutarlo dos veces si hay forwards internos).
//
// Este filtro se ejecuta ANTES que el controller. Su trabajo es simple:
// mirar si el request trae un header "Authorization: Bearer <token>", y
// si el token es válido, avisarle a Spring Security "este request está
// autenticado como tal usuario". Si no hay token, o es inválido, no hace
// nada — deja pasar el request sin autenticar, y es SecurityConfig el que
// decide después si ese endpoint necesitaba estar autenticado o no.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AdminUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AdminUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // saca el "Bearer " de adelante

        try {
            String username = jwtService.extractUsername(token);

            // El chequeo de getAuthentication() == null evita reprocesar
            // si por algún motivo ya se autenticó antes en la misma request.
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            // Token vencido, mal formado, o firmado con otra clave: no
            // autenticamos nada y seguimos. Si el endpoint pedido requería
            // estar logueado, el RestAuthenticationEntryPoint se encarga
            // de devolver el 401 más adelante en la cadena.
        }

        filterChain.doFilter(request, response);
    }
}