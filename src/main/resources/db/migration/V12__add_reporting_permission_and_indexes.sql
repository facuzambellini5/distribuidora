INSERT INTO permissions (code, description)
SELECT 'reports:read', 'Consultar reportes de ventas, productos y rendimiento'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE code = 'reports:read'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'reports:read'
WHERE r.name IN ('OWNER', 'ADMIN', 'VENDEDOR')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );

CREATE INDEX idx_orders_status_delivered_at ON orders (status, delivered_at);
