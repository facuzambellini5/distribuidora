ALTER TABLE orders
    ADD COLUMN delivery_date DATE NULL AFTER truck_id;

UPDATE orders
SET delivery_date = DATE(created_at)
WHERE delivery_date IS NULL;

ALTER TABLE orders
    MODIFY COLUMN delivery_date DATE NOT NULL;

CREATE INDEX idx_orders_truck_date ON orders (truck_id, delivery_date);

INSERT INTO permissions (code, description)
SELECT 'trucks:read', 'Ver camiones'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'trucks:read');

INSERT INTO permissions (code, description)
SELECT 'trucks:write', 'Crear y editar camiones'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'trucks:write');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('trucks:read', 'trucks:write')
WHERE r.name IN ('OWNER', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'trucks:read'
WHERE r.name IN ('DEPOSITO', 'REPARTIDOR')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
