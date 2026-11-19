package com.example.distribuidora.repositories;

import com.example.distribuidora.enums.OrderStatus;
import com.example.distribuidora.models.Order;
import com.example.distribuidora.repositories.projections.EmployeeDeliveryProjection;
import com.example.distribuidora.repositories.projections.EmployeeSalesProjection;
import com.example.distribuidora.repositories.projections.SalesPeriodProjection;
import com.example.distribuidora.repositories.projections.TopSellingProductProjection;
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

    // Filtros opcionales y combinables: los parametros null se ignoran.
    @Query("""
            select o
            from Order o
            where (:customerId is null or o.customer.id = :customerId)
              and (:truckId is null or o.truckId = :truckId)
              and (:status is null or o.status = :status)
            order by o.deliveryDate desc, o.id desc
            """)
    List<Order> search(@Param("customerId") Long customerId,
                       @Param("truckId") Long truckId,
                       @Param("status") OrderStatus status);

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

    @Query(value = """
            select case
                       when :groupBy = 'DAY' then date_format(o.delivered_at, '%Y-%m-%d')
                       when :groupBy = 'WEEK' then date_format(
                           date_sub(date(o.delivered_at), interval weekday(o.delivered_at) day), '%Y-%m-%d')
                       when :groupBy = 'MONTH' then date_format(o.delivered_at, '%Y-%m')
                       when :groupBy = 'CUSTOM' then concat(date_format(:fromDate, '%Y-%m-%d'), ' - ',
                                                           date_format(:toDate, '%Y-%m-%d'))
                       else date_format(o.delivered_at, '%Y')
                   end as period,
                   count(o.id) as orderCount,
                   coalesce(sum(o.total_amount), 0) as salesAmount
            from orders o
            where o.status = 'DELIVERED'
              and o.delivered_at >= :fromDate
              and o.delivered_at < date_add(:toDate, interval 1 day)
            group by period
            order by period
            """, nativeQuery = true)
    List<SalesPeriodProjection> summarizeSales(@Param("fromDate") LocalDate fromDate,
                                               @Param("toDate") LocalDate toDate,
                                               @Param("groupBy") String groupBy);

    @Query(value = """
            select p.id as productId,
                   p.name as productName,
                   sum(i.quantity) as quantitySold,
                   coalesce(sum(i.subtotal), 0) as salesAmount
            from orders o
            join order_items i on i.delivery_order_id = o.id
            join products p on p.id = i.product_id
            where o.status = 'DELIVERED'
              and o.delivered_at >= :fromDate
              and o.delivered_at < date_add(:toDate, interval 1 day)
            group by p.id, p.name
            order by quantitySold desc, salesAmount desc, p.name asc
            limit :limit
            """, nativeQuery = true)
    List<TopSellingProductProjection> findTopSellingProducts(@Param("fromDate") LocalDate fromDate,
                                                             @Param("toDate") LocalDate toDate,
                                                             @Param("limit") int limit);

    @Query(value = """
            select u.id as employeeId,
                   u.full_name as employeeName,
                   r.name as role,
                   count(o.id) as orderCount,
                   coalesce(sum(o.total_amount), 0) as salesAmount
            from orders o
            join users u on u.id = o.created_by
            join roles r on r.id = u.role_id
            where o.status = 'DELIVERED'
              and o.delivered_at >= :fromDate
              and o.delivered_at < date_add(:toDate, interval 1 day)
              and (:role is null or r.name = :role)
            group by u.id, u.full_name, r.name
            """, nativeQuery = true)
    List<EmployeeSalesProjection> summarizeEmployeeSales(@Param("fromDate") LocalDate fromDate,
                                                         @Param("toDate") LocalDate toDate,
                                                         @Param("role") String role);

    @Query(value = """
            select u.id as employeeId,
                   u.full_name as employeeName,
                   r.name as role,
                   count(distinct o.id) as deliveriesCompleted
            from orders o
            join trucks t on t.id = o.truck_id
            join users u on u.id = t.driver_id
            join roles r on r.id = u.role_id
            where o.status = 'DELIVERED'
              and o.delivered_at >= :fromDate
              and o.delivered_at < date_add(:toDate, interval 1 day)
              and (:role is null or r.name = :role)
            group by u.id, u.full_name, r.name
            """, nativeQuery = true)
    List<EmployeeDeliveryProjection> summarizeEmployeeDeliveries(@Param("fromDate") LocalDate fromDate,
                                                                 @Param("toDate") LocalDate toDate,
                                                                 @Param("role") String role);
}
