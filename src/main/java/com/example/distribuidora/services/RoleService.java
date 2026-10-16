package com.example.distribuidora.services;

import com.example.distribuidora.dtos.RoleResponse;
import com.example.distribuidora.repositories.IRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final IRoleRepository roleRepo;

    public RoleService(IRoleRepository roleRepo) {
        this.roleRepo = roleRepo;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepo.findAll().stream().map(RoleResponse::new).toList();
    }
}
