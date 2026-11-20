-- Preserve pre-attempt stock movements and the in-flight orders already in the database.
INSERT INTO order_delivery_attempts (
    delivery_order_id, attempt_number, truck_id, delivery_date, status, attempted_at, completed_at
)
SELECT o.id,
       1,
       o.truck_id,
       o.delivery_date,
       CASE
           WHEN o.status = 'DELIVERED' THEN 'DELIVERED'
           WHEN o.status = 'IN_ROUTE' THEN 'IN_PROGRESS'
           ELSE 'FAILED'
       END,
       COALESCE(MIN(m.created_at), o.delivered_at, o.created_at),
       CASE
           WHEN o.status = 'IN_ROUTE' THEN NULL
           ELSE COALESCE(MAX(m.created_at), o.delivered_at, o.created_at)
       END
FROM orders o
         LEFT JOIN truck_stock_movements m ON m.delivery_order_id = o.id
WHERE (o.status = 'IN_ROUTE' OR m.id IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM order_delivery_attempts existing
      WHERE existing.delivery_order_id = o.id
        AND existing.attempt_number = 1
  )
GROUP BY o.id, o.truck_id, o.delivery_date, o.status, o.created_at, o.delivered_at;

UPDATE truck_stock_movements m
    JOIN order_delivery_attempts a
      ON a.delivery_order_id = m.delivery_order_id
         AND a.attempt_number = 1
SET m.delivery_attempt_id = a.id
WHERE m.delivery_order_id IS NOT NULL
  AND m.delivery_attempt_id IS NULL;

ALTER TABLE order_delivery_attempts
    DROP CHECK chk_delivery_attempt_status_allowed,
    ADD CONSTRAINT chk_delivery_attempt_status_allowed
        CHECK (status IN ('IN_PROGRESS', 'FAILED', 'DELIVERED', 'RETURNED', 'CANCELLED'));

