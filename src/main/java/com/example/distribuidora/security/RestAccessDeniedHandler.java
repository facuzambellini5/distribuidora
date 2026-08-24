package com.example.distribuidora.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

// En vez de armar el JSON a mano con ObjectMapper, se le pasa la
// excepción al mismo resolver que usa Spring MVC para los @ExceptionHandler
// normales. Así el 403 que ve el cliente sale del MISMO lugar que ya
// hay armado en GlobalExceptionHandler.handleAccessDenied(...), en vez
// de duplicar la construcción del ApiErrorResponse acá también.
//
// El @Qualifier es necesario porque Spring registra varios beans de tipo
// HandlerExceptionResolver; "handlerExceptionResolver" (así, el nombre
// exacto) es el que agrupa los que procesan @ExceptionHandler.
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public RestAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        resolver.resolveException(request, response, null, accessDeniedException);
    }
}