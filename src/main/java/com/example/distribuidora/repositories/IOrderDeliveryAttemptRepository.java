package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.DeliveryAttemptStatus;
import com.example.distribuidora.models.OrderDeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOrderDeliveryAttemptRepository extends JpaRepository<OrderDeliveryAttempt, Long> {

    List<OrderDeliveryAttempt> findByOrder_IdOrderByAttemptNumberDesc(Long orderId);

    Optional<OrderDeliveryAttempt> findFirstByOrder_IdAndStatusOrderByAttemptNumberDesc(
            Long orderId, DeliveryAttemptStatus status);

    Optional<OrderDeliveryAttempt> findByIdempotencyKey(String idempotencyKey);

    long countByOrder_Id(Long orderId);
}
