package com.example.distribuidora.repositories;

import com.example.distribuidora.models.OrderStockLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOrderStockLoadRepository extends JpaRepository<OrderStockLoad, Long> {

    Optional<OrderStockLoad> findByOrderIdAndProductId(Long orderId, Long productId);

    List<OrderStockLoad> findByOrderIdAndRevertedAtIsNull(Long orderId);
}
