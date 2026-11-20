package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.LedgerEntryType;
import com.example.distribuidora.models.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ILedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByCustomerIdOrderByCreatedAtAscIdAsc(Long customerId);

    Optional<LedgerEntry> findTopByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    List<LedgerEntry> findByDeliveryOrderIdOrderByCreatedAtAscIdAsc(Long orderId);

    @Query("""
            select count(e) > 0
            from LedgerEntry e
            where e.deliveryOrder.id = :orderId
              and e.entryType = :entryType
            """)
    boolean existsChargeForOrder(@Param("orderId") Long orderId, @Param("entryType") LedgerEntryType entryType);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from LedgerEntry e
            where e.deliveryOrder.id = :orderId
              and e.entryType = com.example.distribuidora.enums.LedgerEntryType.PAYMENT
            """)
    BigDecimal sumPaymentsByOrderId(@Param("orderId") Long orderId);

    @Query("""
            select e.deliveryOrder.id, coalesce(sum(e.amount), 0), max(e.createdAt)
            from LedgerEntry e
            where e.deliveryOrder.id in :orderIds
              and e.entryType = com.example.distribuidora.enums.LedgerEntryType.PAYMENT
            group by e.deliveryOrder.id
            """)
    List<Object[]> summarizePaymentsByOrderIds(@Param("orderIds") List<Long> orderIds);
}
