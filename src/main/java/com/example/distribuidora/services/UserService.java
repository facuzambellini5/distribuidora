package com.example.distribuidora.services;

import com.example.distribuidora.dtos.UserCreateRequest;
import com.example.distribuidora.dtos.UserResponse;
import com.example.distribuidora.dtos.UserUpdateRequest;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Role;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.IRoleRepository;
import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final IUserRepository userRepo;
    private final IRoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(IUserRepository userRepo, IRoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        validateUniqueEmail(request.email(), null);
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(resolveRole(request.roleId()));
        return new UserResponse(userRepo.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userRepo.findAll().stream().map(UserResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return new UserResponse(getUserEntity(id));
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserEntity(id);
        if (request.email() != null) {
            validateUniqueEmail(request.email(), id);
            user.setEmail(request.email());
        }
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.roleId() != null) user.setRole(resolveRole(request.roleId()));
        if (request.active() != null) user.setActive(request.active());
        return new UserResponse(userRepo.save(user));
    }

    @Transactional
    public void deactivate(Long id) {
        User user = getUserEntity(id);
        user.setActive(false);
        userRepo.save(user);
    }

    @Transactional
    public void activate(Long id) {
        User user = getUserEntity(id);
        user.setActive(true);
        userRepo.save(user);
    }

    private User getUserEntity(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    private Role resolveRole(Long id) {
        return roleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Role", id));
    }

    private void validateUniqueEmail(String email, Long currentId) {
        boolean exists = currentId == null
                ? userRepo.existsByEmail(email)
                : userRepo.existsByEmailAndIdNot(email, currentId);
        if (exists) {
            throw new DuplicateResourceException("User email already exists: " + email);
        }
    }
}
