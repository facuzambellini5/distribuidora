package com.example.distribuidora.security;

import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Este es el "puente" entre tu base de datos y Spring Security. El
// framework llama a loadUserByUsername() cada vez que necesita saber
// "¿quién es este usuario y cuál es su contraseña hasheada?" — nosotros
// simplemente le devolvemos el AdminUser tal cual, porque ya implementa
// UserDetails directo.
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final IUserRepository userRepo;

    public AdminUserDetailsService(IUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}