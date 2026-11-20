package com.example.distribuidora.services;

import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final IUserRepository userRepo;

    public CurrentUserService(IUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        if (auth.getPrincipal() instanceof User user) {
            return user;
        }
        String username = auth.getName();
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepo.findByUsername(username).orElse(null);
    }
}
