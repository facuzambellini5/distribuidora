SET @orders_status_check = (
    SELECT tc.constraint_name
    FROM information_schema.table_constraints tc
             JOIN information_schema.check_constraints cc
                  ON cc.constraint_schema = tc.constraint_schema
                      AND cc.constraint_name = tc.constraint_name
    WHERE tc.constraint_schema = DATABASE()
      AND tc.table_name = 'orders'
      AND tc.constraint_type = 'CHECK'
      AND LOWER(cc.check_clause) LIKE '%cancelled%'
    LIMIT 1
);
SET @drop_orders_status_check = IF(
    @orders_status_check IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE orders DROP CHECK `', REPLACE(@orders_status_check, '`', '``'), '`')
);
PREPARE drop_orders_status_check_stmt FROM @drop_orders_status_check;
EXECUTE drop_orders_status_check_stmt;
DEALLOCATE PREPARE drop_orders_status_check_stmt;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status_allowed
        CHECK (status IN ('PENDING', 'IN_ROUTE', 'DELIVERED', 'CANCELLED', 'RETURNED'));

SET @attempt_status_check = (
    SELECT tc.constraint_name
    FROM information_schema.table_constraints tc
             JOIN information_schema.check_constraints cc
                  ON cc.constraint_schema = tc.constraint_schema
                      AND cc.constraint_name = tc.constraint_name
    WHERE tc.constraint_schema = DATABASE()
      AND tc.table_name = 'order_delivery_attempts'
      AND tc.constraint_type = 'CHECK'
      AND LOWER(cc.check_clause) LIKE '%failed%'
    LIMIT 1
);
SET @drop_attempt_status_check = IF(
    @attempt_status_check IS NULL,
    'SELECT 1',
    CONCAT('ALTER TABLE order_delivery_attempts DROP CHECK `', REPLACE(@attempt_status_check, '`', '``'), '`')
);
PREPARE drop_attempt_status_check_stmt FROM @drop_attempt_status_check;
EXECUTE drop_attempt_status_check_stmt;
DEALLOCATE PREPARE drop_attempt_status_check_stmt;

ALTER TABLE order_delivery_attempts
    ADD CONSTRAINT chk_delivery_attempt_status_allowed
        CHECK (status IN ('IN_PROGRESS', 'FAILED', 'DELIVERED', 'RETURNED'));

CREATE INDEX idx_orders_truck_date_status
    ON orders (truck_id, delivery_date, status);
