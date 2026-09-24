package com.example.distribuidora.dtos;

// expiresInSeconds: así el frontend sabe cuánto le dura el token sin
// tener que decodificarlo.
public record LoginResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String username
) {
}