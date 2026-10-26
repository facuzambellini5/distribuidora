-- Pedidos de demostracion para la distribuidora de chacinados.
-- Se asignan a clientes y productos creados en V10. Los pedidos quedan
-- pendientes, sin camion, para no generar movimientos de stock ni pagos.

INSERT INTO orders (customer_id, delivery_address_id, truck_id, delivery_date, status, total_amount)
SELECT customer.id,
       address.id,
       NULL,
       DATE_ADD(CURRENT_DATE, INTERVAL seed.delivery_offset DAY),
       'PENDING',
       seed.total_amount
FROM (
         SELECT 'Almacen Centro Demo' AS business_name, 1 AS delivery_offset, 104000.00 AS total_amount
         UNION ALL SELECT 'Kiosco San Martin Demo', 1, 128700.00
         UNION ALL SELECT 'Autoservicio La Termal Demo', 2, 191100.00
         UNION ALL SELECT 'Despensa Mitre Demo', 2, 115500.00
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo', 3, 149500.00
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo', 3, 127900.00
     ) AS seed
         JOIN customers customer ON customer.business_name = seed.business_name
         JOIN customer_addresses address
              ON address.customer_id = customer.id
                  AND address.is_default = TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM orders existing
    WHERE existing.customer_id = customer.id
      AND existing.delivery_address_id = address.id
      AND existing.delivery_date = DATE_ADD(CURRENT_DATE, INTERVAL seed.delivery_offset DAY)
      AND existing.total_amount = seed.total_amount
);

INSERT INTO order_items (delivery_order_id, product_id, quantity, unit_price, subtotal)
SELECT customer_order.id,
       product.id,
       item.quantity,
       product.unit_price,
       product.unit_price * item.quantity
FROM (
         SELECT 'Almacen Centro Demo' AS business_name, 1 AS delivery_offset, 104000.00 AS order_total, 'EMB-CHO-PUR' AS sku, 6 AS quantity
         UNION ALL SELECT 'Almacen Centro Demo', 1, 104000.00, 'EMB-SAL-PIC', 2
         UNION ALL SELECT 'Kiosco San Martin Demo', 1, 128700.00, 'FIA-JAM-COC', 4
         UNION ALL SELECT 'Kiosco San Martin Demo', 1, 128700.00, 'QUE-BAR', 3
         UNION ALL SELECT 'Kiosco San Martin Demo', 1, 128700.00, 'CON-SAL-VIE6', 10
         UNION ALL SELECT 'Autoservicio La Termal Demo', 2, 191100.00, 'CER-BON-FRE', 8
         UNION ALL SELECT 'Autoservicio La Termal Demo', 2, 191100.00, 'FIA-PAN-AHU', 3
         UNION ALL SELECT 'Autoservicio La Termal Demo', 2, 191100.00, 'ACO-ACE-VER', 2
         UNION ALL SELECT 'Despensa Mitre Demo', 2, 115500.00, 'FIA-MOR-COC', 5
         UNION ALL SELECT 'Despensa Mitre Demo', 2, 115500.00, 'EMB-CHO-PUR', 4
         UNION ALL SELECT 'Despensa Mitre Demo', 2, 115500.00, 'ACO-MOS-250', 10
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo', 3, 149500.00, 'EMB-SAL-PIC', 3
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo', 3, 149500.00, 'FIA-JAM-COC', 3
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo', 3, 149500.00, 'EMB-CHO-PAR', 5
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo', 3, 127900.00, 'CON-SAL-VIE6', 20
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo', 3, 127900.00, 'QUE-CRE', 4
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo', 3, 127900.00, 'ACO-PEP-VIN360', 5
     ) AS item
         JOIN customers customer ON customer.business_name = item.business_name
         JOIN customer_addresses address
              ON address.customer_id = customer.id
                  AND address.is_default = TRUE
         JOIN orders customer_order
              ON customer_order.customer_id = customer.id
                  AND customer_order.delivery_address_id = address.id
                  AND customer_order.delivery_date = DATE_ADD(CURRENT_DATE, INTERVAL item.delivery_offset DAY)
                  AND customer_order.total_amount = item.order_total
                  AND customer_order.status = 'PENDING'
         JOIN products product ON product.sku = item.sku
WHERE NOT EXISTS (
    SELECT 1
    FROM order_items existing
    WHERE existing.delivery_order_id = customer_order.id
      AND existing.product_id = product.id
);
