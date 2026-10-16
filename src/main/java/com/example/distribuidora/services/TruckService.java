package com.example.distribuidora.services;

import com.example.distribuidora.dtos.TruckRequest;
import com.example.distribuidora.dtos.TruckResponse;
import com.example.distribuidora.exceptions.BusinessRuleException;
import com.example.distribuidora.exceptions.DuplicateResourceException;
import com.example.distribuidora.exceptions.EntityNotFoundException;
import com.example.distribuidora.models.Truck;
import com.example.distribuidora.models.User;
import com.example.distribuidora.repositories.ITruckRepository;
import com.example.distribuidora.repositories.IUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TruckService {

    private static final String DRIVER_ROLE = "REPARTIDOR";

    private final ITruckRepository truckRepo;
    private final IUserRepository userRepo;

    public TruckService(ITruckRepository truckRepo, IUserRepository userRepo) {
        this.truckRepo = truckRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public TruckResponse save(TruckRequest request) {

        String plate = this.normalizePlate(request.licensePlate());

        if (truckRepo.existsByLicensePlate(plate)) {
            throw new DuplicateResourceException("A truck with license plate '" + plate + "' already exists");
        }

        Truck truck = new Truck();
        truck.setLicensePlate(plate);
        truck.setDriver(this.resolveDriver(request.driverId()));

        return new TruckResponse(truckRepo.save(truck));
    }

    @Transactional(readOnly = true)
    public List<TruckResponse> getTrucks() {
        return truckRepo.findAll().stream().map(TruckResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public List<TruckResponse> getActiveTrucks() {
        return truckRepo.findByIsActiveTrue().stream().map(TruckResponse::new).toList();
    }

    @Transactional(readOnly = true)
    public TruckResponse getTruckById(Long id) {
        return new TruckResponse(this.findTruck(id));
    }

    //PUT: reemplaza patente y chofer. Si driverId viene null, el camion queda sin chofer
    @Transactional
    public TruckResponse update(Long id, TruckRequest request) {

        Truck truck = this.findTruck(id);
        String plate = this.normalizePlate(request.licensePlate());

        if (truckRepo.existsByLicensePlateAndIdNot(plate, id)) {
            throw new DuplicateResourceException("A truck with license plate '" + plate + "' already exists");
        }

        truck.setLicensePlate(plate);
        truck.setDriver(this.resolveDriver(request.driverId()));

        return new TruckResponse(truckRepo.save(truck));
    }

    @Transactional
    public void desactivate(Long id) {
        Truck truck = this.findTruck(id);
        truck.setActive(false);
        truckRepo.save(truck);
    }

    @Transactional
    public void activate(Long id) {
        Truck truck = this.findTruck(id);
        truck.setActive(true);
        truckRepo.save(truck);
    }

    @Transactional
    public void delete(Long id) {
        desactivate(id);
    }

    @Transactional(readOnly = true)
    public Truck findTruck(Long id) {
        return truckRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Truck", id));
    }

    //La patente se guarda en mayusculas y sin espacios, asi "ab 123 cd" y "AB123CD" son la misma
    private String normalizePlate(String plate) {
        return plate.replaceAll("\\s+", "").toUpperCase();
    }

    //El chofer tiene que existir, estar activo y tener rol REPARTIDOR
    private User resolveDriver(Long driverId) {
        if (driverId == null) return null;

        User driver = userRepo.findById(driverId)
                .orElseThrow(() -> new EntityNotFoundException("User", driverId));

        if (!driver.isActive()) {
            throw new BusinessRuleException("User " + driverId + " is not active");
        }
        if (!DRIVER_ROLE.equals(driver.getRole().getName())) {
            throw new BusinessRuleException("User " + driverId + " doesn't have the role " + DRIVER_ROLE);
        }
        return driver;
    }
}
