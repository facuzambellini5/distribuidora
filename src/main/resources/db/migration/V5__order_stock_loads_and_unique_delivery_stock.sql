-- 1) delivery_stock: unificar duplicados (camion, producto, fecha) y agregar la restriccion unica
UPDATE delivery_stock ds
    JOIN (SELECT MIN(id)                AS keep_id,
                 SUM(quantity_loaded)   AS loaded,
                 SUM(quantity_returned) AS returned
          FROM delivery_stock
          GROUP BY truck_id, product_id, delivery_date
          HAVING COUNT(*) > 1) d ON ds.id = d.keep_id
SET ds.quantity_loaded   = d.loaded,
    ds.quantity_returned = d.returned;

DELETE ds
FROM delivery_stock ds
         JOIN (SELECT MIN(id) AS keep_id, truck_id, product_id, delivery_date
               FROM delivery_stock
               GROUP BY truck_id, product_id, delivery_date
               HAVING COUNT(*) > 1) d
              ON ds.truck_id = d.truck_id
                  AND ds.product_id = d.product_id
                  AND ds.delivery_date = d.delivery_date
                  AND ds.id <> d.keep_id;

ALTER TABLE delivery_stock
    ADD CONSTRAINT uq_delivery_stock_truck_product_date UNIQUE (truck_id, product_id, delivery_date);

-- 2) Cargas de stock originadas por un pedido.
--    Una fila por (pedido, producto): la clave unica evita duplicar la carga ante reintentos.
--    reverted_at NULL = carga vigente; con valor = carga revertida (cancelacion / desasignacion / reasignacion).
CREATE TABLE order_stock_loads
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id BIGINT   NOT NULL,
    product_id        BIGINT   NOT NULL,
    truck_id          BIGINT   NOT NULL,
    delivery_date     DATE     NOT NULL,
    quantity          INT      NOT NULL CHECK (quantity > 0),
    reverted_at       DATETIME NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_stock_loads_order_product UNIQUE (delivery_order_id, product_id),
    CONSTRAINT fk_order_stock_loads_order
        FOREIGN KEY (delivery_order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_stock_loads_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_order_stock_loads_truck
        FOREIGN KEY (truck_id) REFERENCES trucks (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_order_stock_loads_active ON order_stock_loads (delivery_order_id, reverted_at);
