package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.StockMovementType;
import com.example.distribuidora.models.TruckStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ITruckStockMovementRepository extends JpaRepository<TruckStockMovement, Long> {

    Optional<TruckStockMovement> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select coalesce(sum(m.quantity), 0)
            from TruckStockMovement m
            where m.deliveryOrder.id = :orderId
              and m.product.id = :productId
              and m.movementType = :movementType
            """)
    Integer sumQuantityByOrderAndProductAndType(@Param("orderId") Long orderId,
                                                @Param("productId") Long productId,
                                                @Param("movementType") StockMovementType movementType);

    @Query("""
            select coalesce(sum(m.quantity), 0)
            from TruckStockMovement m
            where m.deliveryAttempt.id = :attemptId
              and m.product.id = :productId
              and m.movementType = :movementType
            """)
    Integer sumQuantityByAttemptAndProductAndType(@Param("attemptId") Long attemptId,
                                                   @Param("productId") Long productId,
                                                   @Param("movementType") StockMovementType movementType);

    @Query(value = """
            select coalesce(max(attempt_returns.returned_quantity), 0)
            from (
                select coalesce(m.delivery_attempt_id, 0) as attempt_id,
                       sum(m.quantity) as returned_quantity
                from truck_stock_movements m
                where m.delivery_order_id = :orderId
                  and m.product_id = :productId
                  and m.truck_id = :truckId
                  and m.movement_date = :movementDate
                  and m.movement_type = 'RETURN'
                group by coalesce(m.delivery_attempt_id, 0)
            ) attempt_returns
            """, nativeQuery = true)
    Integer maxReturnedQuantityByOrderProductTruckAndDate(@Param("orderId") Long orderId,
                                                           @Param("productId") Long productId,
                                                           @Param("truckId") Long truckId,
                                                           @Param("movementDate") LocalDate movementDate);

    @Query("""
            select coalesce(sum(m.quantity), 0)
            from TruckStockMovement m
            where m.truck.id = :truckId
              and m.product.id = :productId
              and m.movementDate = :movementDate
              and m.movementType = :movementType
            """)
    Integer sumQuantityByTruckProductDateAndType(@Param("truckId") Long truckId,
                                                 @Param("productId") Long productId,
                                                 @Param("movementDate") LocalDate movementDate,
                                                 @Param("movementType") StockMovementType movementType);

    @Query("""
            select coalesce(sum(m.quantity), 0)
            from TruckStockMovement m
            where m.deliveryOrder.id = :orderId
              and m.truck.id = :truckId
              and m.product.id = :productId
              and m.movementDate = :movementDate
              and m.movementType = :movementType
            """)
    Integer sumQuantityByOrderTruckProductDateAndType(@Param("orderId") Long orderId,
                                                      @Param("truckId") Long truckId,
                                                      @Param("productId") Long productId,
                                                      @Param("movementDate") LocalDate movementDate,
                                                      @Param("movementType") StockMovementType movementType);

    @Query("""
            select m
            from TruckStockMovement m
            join fetch m.truck t
            join fetch m.product p
            left join fetch m.deliveryOrder o
            left join fetch m.createdBy u
            where (:truckId is null or t.id = :truckId)
              and (:productId is null or p.id = :productId)
              and (:fromDate is null or m.movementDate >= :fromDate)
              and (:toDate is null or m.movementDate <= :toDate)
              and (:userId is null or u.id = :userId)
              and (:movementType is null or m.movementType = :movementType)
            order by m.createdAt desc, m.id desc
            """)
    List<TruckStockMovement> search(@Param("truckId") Long truckId,
                                    @Param("productId") Long productId,
                                    @Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate,
                                    @Param("userId") Long userId,
                                    @Param("movementType") StockMovementType movementType);

    @Query("""
            select m
            from TruckStockMovement m
            join fetch m.truck t
            join fetch m.product p
            join fetch m.deliveryOrder o
            left join fetch m.createdBy u
            where o.customer.id = :customerId
              and m.movementType = com.example.distribuidora.enums.StockMovementType.RETURN
            order by m.createdAt desc, m.id desc
            """)
    List<TruckStockMovement> findReturnsByCustomerId(@Param("customerId") Long customerId);

    @Query("""
            select m
            from TruckStockMovement m
            join fetch m.truck t
            join fetch m.product p
            left join fetch m.createdBy u
            where m.deliveryOrder.id = :orderId
              and m.movementType = com.example.distribuidora.enums.StockMovementType.RETURN
            order by m.createdAt asc, m.id asc
            """)
    List<TruckStockMovement> findReturnsByOrderId(@Param("orderId") Long orderId);
}
