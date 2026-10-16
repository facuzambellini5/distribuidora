package com.example.distribuidora.services;

import com.example.distribuidora.dtos.LoginRequest;
import com.example.distribuidora.dtos.LoginResponse;
import com.example.distribuidora.dtos.MeResponse;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ITruckRepository;
import com.example.distribuidora.repositories.IUserRepository;
import com.example.distribuidora.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final IUserRepository userRepo;
    private final ITruckRepository truckRepo;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, IUserRepository userRepo,
                       ITruckRepository truckRepo, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
        this.truckRepo = truckRepo;
        this.jwtService = jwtService;
    }

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

    // Datos del usuario logueado. Se relee de la base (no del token) para que
    // rol, permisos y camion asignado reflejen el estado actual.
    @Transactional(readOnly = true)
    public MeResponse me(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User", username));
        Truck truck = truckRepo.findFirstByDriverIdAndIsActiveTrue(user.getId()).orElse(null);
        return new MeResponse(user, truck);
    }
}
