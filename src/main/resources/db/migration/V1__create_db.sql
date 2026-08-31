-- =====================================================================
-- V1__initial_schema_cafe.sql
-- Esquema inicial - Distribuidora
--
-- Roles y permisos: predefinidos, cargados por este mismo script.
-- El dueño de la distribuidora NO los administra desde la app,
-- por eso van directo acá como datos semilla (seed data).
-- =====================================================================

-- ---------------------------------------------------------------------
-- IAM: roles, permisos y la relación fija entre ambos
-- ---------------------------------------------------------------------

CREATE TABLE roles
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(30) NOT NULL UNIQUE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE permissions
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- formato "recurso:accion", ej: 'products:read', 'products:write'
    code        VARCHAR(60) NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE role_permissions
(
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id       BIGINT       NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- Catálogo: categorías (jerarquía máx. 2 niveles, se valida en el
-- service, no acá) y productos de café
-- ---------------------------------------------------------------------

CREATE TABLE product_categories
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100) NOT NULL,
    parent_category_id BIGINT,
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_category_id) REFERENCES product_categories (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE products
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id      BIGINT,
    name             VARCHAR(150)   NOT NULL,
    description      TEXT,
    sku              VARCHAR(50) UNIQUE,
    roast_level      VARCHAR(20) CHECK (roast_level IN ('LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK')),
    grind_type       VARCHAR(20) CHECK (grind_type IN ('WHOLE_BEAN', 'GROUND_FINE', 'GROUND_MEDIUM', 'GROUND_COARSE')),
    net_weight_grams INT            NOT NULL,
    unit_price       DECIMAL(12, 2) NOT NULL,
    -- solo la URL; el archivo real vive en Cloudflare R2
    photo_url        VARCHAR(500),
    is_active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_products_category ON products (category_id);

-- ---------------------------------------------------------------------
-- Clientes y direcciones de entrega (1 a muchos, con lat/lng)
-- ---------------------------------------------------------------------

CREATE TABLE customers
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_name VARCHAR(150) NOT NULL,
    tax_id        VARCHAR(30),
    name          VARCHAR(150),
    phone         VARCHAR(30),
    email         VARCHAR(150),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE customer_addresses
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT       NOT NULL,
    label          VARCHAR(80),
    street_address VARCHAR(255) NOT NULL,
    city           VARCHAR(100),
    latitude       DECIMAL(10, 7),
    longitude      DECIMAL(10, 7),
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_address_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_customer_addresses_customer ON customer_addresses (customer_id);

-- ---------------------------------------------------------------------
-- Logística: camiones, stock cargado, pedidos de entrega
-- ---------------------------------------------------------------------

CREATE TABLE trucks
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    driver_id     BIGINT,
    is_active     BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_truck_driver
        FOREIGN KEY (driver_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE delivery_orders
(
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id               BIGINT         NOT NULL,
    truck_id                  BIGINT,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'IN_ROUTE', 'DELIVERED', 'CANCELLED')),
    -- snapshot de la dirección al momento del pedido (offline-safe)
    delivery_address_snapshot TEXT           NOT NULL,
    delivery_latitude         DECIMAL(10, 7),
    delivery_longitude        DECIMAL(10, 7),
    total_amount              DECIMAL(12, 2) NOT NULL,
    created_by                BIGINT,
    created_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at              TIMESTAMP      NULL,
    CONSTRAINT fk_order_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_order_truck
        FOREIGN KEY (truck_id) REFERENCES trucks (id),
    CONSTRAINT fk_order_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_delivery_orders_customer ON delivery_orders (customer_id);
CREATE INDEX idx_delivery_orders_status ON delivery_orders (status);

CREATE TABLE order_items
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id BIGINT         NOT NULL,
    product_id        BIGINT         NOT NULL,
    quantity          INT            NOT NULL CHECK (quantity > 0),
    -- precio al momento del pedido, no el precio actual del producto
    unit_price        DECIMAL(12, 2) NOT NULL,
    subtotal          DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_item_order
        FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_order_items_order ON order_items (delivery_order_id);

CREATE TABLE delivery_stock
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    truck_id          BIGINT NOT NULL,
    product_id        BIGINT NOT NULL,
    delivery_date     DATE   NOT NULL,
    quantity_loaded   INT    NOT NULL DEFAULT 0,
    quantity_returned INT    NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_truck
        FOREIGN KEY (truck_id) REFERENCES trucks (id),
    CONSTRAINT fk_stock_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_delivery_stock_truck_date ON delivery_stock (truck_id, delivery_date);

-- ---------------------------------------------------------------------
-- Cuenta corriente del cliente: ledger inmutable, saldo pre-calculado
-- (nunca se hace UPDATE sobre estas filas, solo INSERT)
-- ---------------------------------------------------------------------

CREATE TABLE ledger_entries
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id       BIGINT         NOT NULL,
    delivery_order_id BIGINT,
    entry_type        VARCHAR(20)    NOT NULL CHECK (entry_type IN ('CHARGE', 'PAYMENT', 'ADJUSTMENT')),
    amount            DECIMAL(12, 2) NOT NULL,
    running_balance   DECIMAL(12, 2) NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_ledger_order
        FOREIGN KEY (delivery_order_id) REFERENCES delivery_orders (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_ledger_customer ON ledger_entries (customer_id, created_at);

-- =====================================================================
-- SEED DATA: roles, permisos y su relación fija
-- =====================================================================

INSERT INTO roles (name)
VALUES ('OWNER'),
       ('ADMIN'),
       ('VENDEDOR'),
       ('REPARTIDOR'),
       ('DEPOSITO');

INSERT INTO permissions (code, description)
VALUES ('products:read', 'Ver catálogo de productos'),
       ('products:write', 'Crear y editar productos'),
       ('customers:read', 'Ver clientes'),
       ('customers:write', 'Crear y editar clientes'),
       ('orders:read', 'Ver pedidos'),
       ('orders:write', 'Crear y editar pedidos'),
       ('orders:deliver', 'Marcar pedidos como entregados'),
       ('stock:read', 'Ver stock cargado en camión'),
       ('stock:write', 'Cargar y descargar stock de camión'),
       ('ledger:read', 'Ver cuenta corriente de clientes'),
       ('ledger:write', 'Registrar pagos y ajustes'),
       ('users:manage', 'Crear y editar usuarios del sistema');

-- OWNER y ADMIN: acceso total (se listan explícitamente, sin casos
-- especiales en el código; así el chequeo de permisos es siempre igual)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'OWNER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'ADMIN';

-- VENDEDOR: catálogo, clientes, pedidos, cuenta corriente (solo lectura)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'VENDEDOR'
  AND p.code IN ('products:read', 'customers:read', 'customers:write',
                 'orders:read', 'orders:write', 'ledger:read');

-- REPARTIDOR: sus pedidos y el stock que lleva en el camión
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'REPARTIDOR'
  AND p.code IN ('orders:read', 'orders:deliver', 'stock:read', 'stock:write');

-- DEPOSITO: productos y stock
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r,
     permissions p
WHERE r.name = 'DEPOSITO'
  AND p.code IN ('products:read', 'stock:read', 'stock:write');