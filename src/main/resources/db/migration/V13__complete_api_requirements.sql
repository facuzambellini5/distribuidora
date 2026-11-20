-- =====================================================================
-- V13__complete_api_requirements.sql
-- Completa esquema para:
-- 1. Historial inmutable de movimientos de stock y devoluciones por camion
-- 2. Pagos con medio de pago, desglose mixto, usuario responsable e idempotencia
-- 3. Descripcion de roles, nombre legible de permisos y permiso products:write a DEPOSITO
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) Roles y permisos: metadata legible + products:write para DEPOSITO
-- ---------------------------------------------------------------------

ALTER TABLE roles
    ADD COLUMN description VARCHAR(255) NULL AFTER name;

UPDATE roles
SET description = CASE name
    WHEN 'OWNER' THEN 'Dueño de la distribuidora con acceso total al sistema'
    WHEN 'ADMIN' THEN 'Administrador con gestión integral de operaciones y usuarios'
    WHEN 'VENDEDOR' THEN 'Gestión de clientes, toma de pedidos y consulta de ventas y cuenta corriente'
    WHEN 'REPARTIDOR' THEN 'Gestión de entregas asignadas, cobros en reparto y stock de su camión'
    WHEN 'DEPOSITO' THEN 'Gestión de catálogo (productos, categorías, precios) y control de stock por camión'
    ELSE CONCAT('Rol ', name)
END;

ALTER TABLE permissions
    ADD COLUMN name VARCHAR(100) NULL AFTER code;

UPDATE permissions
SET name = CASE code
    WHEN 'products:read' THEN 'Ver productos y categorías'
    WHEN 'products:write' THEN 'Administrar productos, categorías y precios'
    WHEN 'customers:read' THEN 'Ver clientes y direcciones'
    WHEN 'customers:write' THEN 'Administrar clientes y direcciones'
    WHEN 'orders:read' THEN 'Ver pedidos y repartos'
    WHEN 'orders:write' THEN 'Crear, editar y cancelar pedidos'
    WHEN 'orders:deliver' THEN 'Registrar entregas y cobros de pedidos'
    WHEN 'stock:read' THEN 'Ver stock y movimientos de camiones'
    WHEN 'stock:write' THEN 'Cargar stock y registrar devoluciones en camiones'
    WHEN 'ledger:read' THEN 'Ver cuenta corriente y pagos de clientes'
    WHEN 'ledger:write' THEN 'Registrar pagos y ajustes de cuenta corriente'
    WHEN 'users:manage' THEN 'Administrar usuarios y consultar roles'
    WHEN 'trucks:read' THEN 'Ver camiones asignados'
    WHEN 'trucks:write' THEN 'Administrar camiones y choferes'
    WHEN 'reports:read' THEN 'Consultar reportes e historiales de ventas'
    ELSE code
END;

-- Otorgar products:write a DEPOSITO para crear/editar productos, categorias y precios
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'products:write'
WHERE r.name = 'DEPOSITO'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

-- ---------------------------------------------------------------------
-- 2) Historial inmutable de movimientos de stock por camion (cargas, entregas, devoluciones)
-- ---------------------------------------------------------------------

CREATE TABLE truck_stock_movements
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    truck_id          BIGINT       NOT NULL,
    product_id        BIGINT       NOT NULL,
    delivery_order_id BIGINT       NULL,
    movement_type     VARCHAR(20)  NOT NULL CHECK (movement_type IN ('LOAD', 'DELIVERY', 'RETURN')),
    quantity          INT          NOT NULL CHECK (quantity > 0),
    movement_date     DATE         NOT NULL,
    created_by        BIGINT       NULL,
    notes             VARCHAR(255) NULL,
    idempotency_key   VARCHAR(100) NULL UNIQUE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tsm_truck
        FOREIGN KEY (truck_id) REFERENCES trucks (id),
    CONSTRAINT fk_tsm_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_tsm_order
        FOREIGN KEY (delivery_order_id) REFERENCES orders (id),
    CONSTRAINT fk_tsm_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_tsm_truck_date ON truck_stock_movements (truck_id, movement_date);
CREATE INDEX idx_tsm_order_product_type ON truck_stock_movements (delivery_order_id, product_id, movement_type);
CREATE INDEX idx_tsm_product ON truck_stock_movements (product_id);
CREATE INDEX idx_tsm_created_by ON truck_stock_movements (created_by);

-- ---------------------------------------------------------------------
-- 3) Pagos, entregas, usuario responsable e idempotencia
-- ---------------------------------------------------------------------

ALTER TABLE orders
    ADD COLUMN delivered_by BIGINT NULL AFTER delivered_at,
    ADD COLUMN delivery_idempotency_key VARCHAR(100) NULL UNIQUE AFTER transfer_amount,
    ADD CONSTRAINT fk_orders_delivered_by FOREIGN KEY (delivered_by) REFERENCES users (id);

ALTER TABLE ledger_entries
    ADD COLUMN payment_method VARCHAR(32) NULL AFTER entry_type,
    ADD COLUMN cash_amount DECIMAL(12, 2) NULL AFTER payment_method,
    ADD COLUMN transfer_amount DECIMAL(12, 2) NULL AFTER cash_amount,
    ADD COLUMN created_by BIGINT NULL AFTER running_balance,
    ADD COLUMN notes VARCHAR(255) NULL AFTER created_by,
    ADD COLUMN idempotency_key VARCHAR(100) NULL UNIQUE AFTER notes,
    ADD CONSTRAINT fk_ledger_created_by FOREIGN KEY (created_by) REFERENCES users (id);

CREATE INDEX idx_ledger_order_type ON ledger_entries (delivery_order_id, entry_type);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_orders_delivery_date ON orders (delivery_date);
