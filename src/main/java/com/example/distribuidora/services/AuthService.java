package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LoginRequest;
import com.example.distribuidora.dtos.LoginResponse;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.IUserRepository;
import com.example.distribuidora.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private  IUserRepository userRepo;
    @Autowired
    private JwtService jwtService;


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Valida usuario + contraseña: por
        // detrás, Spring usa el DaoAuthenticationProvider de la
        // SecurityConfig, que busca el AdminUser y compara el hash con
        // BCrypt. Si algo no matchea (usuario no existe O contraseña mal),
        // tira BadCredentialsException -> la atrapa el GlobalExceptionHandler
        // y le devuelve al frontend un 401 genérico, sin decir cuál de las
        // dos cosas falló.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // Si llega hasta acá, authenticate() ya confirmó que el usuario
        // existe, así que este findByUsername no debería fallar nunca.
        User user = userRepo.findByUsername(request.username()).orElseThrow();

        String token = jwtService.generateToken(user);
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new LoginResponse(token, "Bearer", expiresInSeconds, user.getUsername());
    }
}