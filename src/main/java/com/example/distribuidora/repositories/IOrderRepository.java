package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IOrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByStatus(OrderStatus status);

    @Query("""
            select coalesce(sum(i.quantity), 0)
            from Order o
            join o.items i
            where o.truckId = :truckId
              and o.deliveryDate = :deliveryDate
              and i.product.id = :productId
              and o.status <> com.example.distribuidora.enums.OrderStatus.CANCELLED
              and (:excludedOrderId is null or o.id <> :excludedOrderId)
            """)
    Integer sumReservedQuantity(@Param("truckId") Long truckId,
                                @Param("deliveryDate") LocalDate deliveryDate,
                                @Param("productId") Long productId,
                                @Param("excludedOrderId") Long excludedOrderId);
}
