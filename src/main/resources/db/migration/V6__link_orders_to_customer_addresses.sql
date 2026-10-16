-- Link each existing order to the customer's saved address before removing
-- the denormalized address snapshot columns.
SET @add_delivery_address_id = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'orders'
          AND column_name = 'delivery_address_id'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD COLUMN delivery_address_id BIGINT NULL AFTER customer_id'
);
PREPARE add_delivery_address_id_stmt FROM @add_delivery_address_id;
EXECUTE add_delivery_address_id_stmt;
DEALLOCATE PREPARE add_delivery_address_id_stmt;

-- Prefer an exact match with the address text saved on the order.
UPDATE orders o
SET delivery_address_id = (
    SELECT a.id
    FROM customer_addresses a
    WHERE a.customer_id = o.customer_id
      AND CONCAT(
            CASE
                WHEN a.label IS NULL OR TRIM(a.label) = '' THEN ''
                ELSE CONCAT(a.label, ' - ')
            END,
            a.street_address,
            CASE
                WHEN a.city IS NULL OR TRIM(a.city) = '' THEN ''
                ELSE CONCAT(', ', a.city)
            END
          ) = o.delivery_address_snapshot
    ORDER BY a.is_default DESC, a.id
    LIMIT 1
);

-- Older snapshots may have been formatted differently. For those, use the
-- customer's default address, or their first saved address as a last resort.
UPDATE orders o
SET delivery_address_id = (
    SELECT a.id
    FROM customer_addresses a
    WHERE a.customer_id = o.customer_id
    ORDER BY a.is_default DESC, a.id
    LIMIT 1
)
WHERE o.delivery_address_id IS NULL;

-- Addresses can have been deleted after an order was created. If the customer
-- has no remaining address, preserve the old order text in a dedicated address
-- row so the required foreign key can still be established.
INSERT INTO customer_addresses (customer_id, label, street_address, latitude, longitude, is_default)
SELECT o.customer_id,
       'Migrated from order',
       LEFT(o.delivery_address_snapshot, 255),
       o.delivery_latitude,
       o.delivery_longitude,
       FALSE
FROM orders o
WHERE o.delivery_address_id IS NULL;

UPDATE orders o
SET delivery_address_id = (
    SELECT a.id
    FROM customer_addresses a
    WHERE a.customer_id = o.customer_id
      AND a.label = 'Migrated from order'
      AND a.street_address = LEFT(o.delivery_address_snapshot, 255)
    ORDER BY a.id
    LIMIT 1
)
WHERE o.delivery_address_id IS NULL;

ALTER TABLE orders
    MODIFY COLUMN delivery_address_id BIGINT NOT NULL;

SET @add_delivery_address_fk = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'orders'
          AND column_name = 'delivery_address_id'
          AND referenced_table_name = 'customer_addresses'
    ),
    'SELECT 1',
    'ALTER TABLE orders ADD CONSTRAINT fk_order_delivery_address FOREIGN KEY (delivery_address_id) REFERENCES customer_addresses (id)'
);
PREPARE add_delivery_address_fk_stmt FROM @add_delivery_address_fk;
EXECUTE add_delivery_address_fk_stmt;
DEALLOCATE PREPARE add_delivery_address_fk_stmt;

ALTER TABLE orders
    DROP COLUMN delivery_address_snapshot,
    DROP COLUMN delivery_latitude,
    DROP COLUMN delivery_longitude;
