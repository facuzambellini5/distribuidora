package com.example.distribuidora.services;

import com.example.distribuidora.dtos.MeResponse;
import com.example.distribuidora.models.Role;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ITruckRepository;
import com.example.distribuidora.repositories.IUserRepository;
import com.example.distribuidora.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceMeTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private IUserRepository userRepo;
    @Mock
    private ITruckRepository truckRepo;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User driver() {
        Role role = new Role();
        role.setId(4L);
        role.setName("REPARTIDOR");
        User user = new User();
        user.setId(7L);
        user.setFullName("Juan Perez");
        user.setEmail("juan@mail.com");
        user.setRole(role);
        return user;
    }

    @Test
    void meReturnsRoleAndAssignedTruck() {
        User user = driver();
        Truck truck = new Truck();
        truck.setId(3L);
        truck.setLicensePlate("AB123CD");
        when(userRepo.findByUsername("juan@mail.com")).thenReturn(Optional.of(user));
        when(truckRepo.findFirstByDriverIdAndIsActiveTrue(7L)).thenReturn(Optional.of(truck));

        MeResponse me = authService.me("juan@mail.com");

        assertEquals(4L, me.roleId());
        assertEquals("REPARTIDOR", me.roleName());
        assertEquals(3L, me.truckId());
        assertEquals("AB123CD", me.truckLicensePlate());
    }

    @Test
    void meHasNoTruckWhenUserIsNotAssigned() {
        when(userRepo.findByUsername("juan@mail.com")).thenReturn(Optional.of(driver()));
        when(truckRepo.findFirstByDriverIdAndIsActiveTrue(7L)).thenReturn(Optional.empty());

        MeResponse me = authService.me("juan@mail.com");

        assertNull(me.truckId());
        assertNull(me.truckLicensePlate());
    }
}
