package com.example.distribuidora.repositories;

import com.example.distribuidora.models.DeliveryStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IDeliveryStockRepository extends JpaRepository<DeliveryStock, Long> {

    List<DeliveryStock> findByTruck_IdAndDeliveryDate(Long truckId, LocalDate deliveryDate);

    List<DeliveryStock> findByTruck_Id(Long truckId);

    boolean existsByTruck_IdAndProduct_IdAndDeliveryDate(Long truckId, Long productId, LocalDate deliveryDate);

    // Bloquea la fila para que cargas/reversiones concurrentes del mismo camion/producto/fecha no se pisen.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from DeliveryStock s
            where s.truck.id = :truckId
              and s.product.id = :productId
              and s.deliveryDate = :deliveryDate
            """)
    Optional<DeliveryStock> findForUpdate(@Param("truckId") Long truckId,
                                          @Param("productId") Long productId,
                                          @Param("deliveryDate") LocalDate deliveryDate);
}
