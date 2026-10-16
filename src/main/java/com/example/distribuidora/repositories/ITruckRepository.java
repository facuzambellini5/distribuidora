package com.example.distribuidora.repositories;

import com.example.distribuidora.models.Truck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITruckRepository extends JpaRepository<Truck, Long> {

    List<Truck> findByIsActiveTrue();

    // Camion activo asignado a un chofer (para /api/auth/me)
    Optional<Truck> findFirstByDriverIdAndIsActiveTrue(Long driverId);

    boolean existsByLicensePlate(String licensePlate);

    //Para el update: que la patente no choque con la de OTRO camion (el mismo camion puede conservar la suya)
    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);
}
