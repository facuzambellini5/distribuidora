package com.example.distribuidora.repositories;

import com.example.distribuidora.models.DeliveryStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IDeliveryStockRepository extends JpaRepository<DeliveryStock, Long> {

    List<DeliveryStock> findByTruck_IdAndDeliveryDate(Long truckId, LocalDate deliveryDate);

    List<DeliveryStock> findByTruck_Id(Long truckId);

    boolean existsByTruck_IdAndProduct_IdAndDeliveryDate(Long truckId, Long productId, LocalDate deliveryDate);
}
