-- Datos iniciales de ejemplo para una distribuidora de chacinados porcinos,
-- fiambres y productos relacionados en Presidencia Roque Sáenz Peña, Chaco.
-- Clientes, domicilios y patentes son ficticios (DEMO). Los precios son
-- ilustrativos en ARS y deben actualizarse antes de operar comercialmente.

INSERT INTO categories (name, parent_category_id)
SELECT root.name, NULL
FROM (
         SELECT 'Cerdo y cortes' AS name
         UNION ALL SELECT 'Embutidos y chacinados'
         UNION ALL SELECT 'Fiambres'
         UNION ALL SELECT 'Quesos y refrigerados'
         UNION ALL SELECT 'Congelados y preparados'
         UNION ALL SELECT 'Acompañamientos'
     ) AS root
WHERE NOT EXISTS (
    SELECT 1 FROM categories existing
    WHERE existing.name = root.name AND existing.parent_category_id IS NULL
);

INSERT INTO categories (name, parent_category_id)
SELECT child.name, parent.id
FROM (
         SELECT 'Cortes frescos' AS name, 'Cerdo y cortes' AS parent_name
         UNION ALL SELECT 'Embutidos frescos', 'Embutidos y chacinados'
         UNION ALL SELECT 'Salames y salamines', 'Embutidos y chacinados'
         UNION ALL SELECT 'Jamones y paletas', 'Fiambres'
         UNION ALL SELECT 'Fiambres cocidos', 'Fiambres'
         UNION ALL SELECT 'Pancetas y ahumados', 'Fiambres'
         UNION ALL SELECT 'Quesos', 'Quesos y refrigerados'
         UNION ALL SELECT 'Congelados', 'Congelados y preparados'
         UNION ALL SELECT 'Aceitunas y encurtidos', 'Acompañamientos'
         UNION ALL SELECT 'Aderezos y complementos', 'Acompañamientos'
     ) AS child
         JOIN (
             SELECT name, MIN(id) AS id
             FROM categories
             WHERE parent_category_id IS NULL
             GROUP BY name
         ) parent ON parent.name = child.parent_name
WHERE NOT EXISTS (
    SELECT 1 FROM categories existing
    WHERE existing.name = child.name AND existing.parent_category_id = parent.id
);

INSERT INTO products (category_id, name, description, sku, unit_price, unit, is_active)
SELECT category.id, product.name, product.description, product.sku, product.unit_price, product.unit, TRUE
FROM (
         SELECT 'Cortes frescos' AS category_name, 'Bondiola de cerdo fresca' AS name, 'Precio de referencia por kilogramo en ARS.' AS description, 'CER-BON-FRE' AS sku, 14500.00 AS unit_price, 'KILOGRAM' AS unit
         UNION ALL SELECT 'Cortes frescos', 'Carré de cerdo deshuesado', 'Precio de referencia por kilogramo en ARS.', 'CER-CAR-DES', 13200.00, 'KILOGRAM'
         UNION ALL SELECT 'Cortes frescos', 'Costillitas de cerdo', 'Precio de referencia por kilogramo en ARS.', 'CER-COS-CER', 11900.00, 'KILOGRAM'
         UNION ALL SELECT 'Cortes frescos', 'Pechito de cerdo', 'Precio de referencia por kilogramo en ARS.', 'CER-PEC-CER', 10800.00, 'KILOGRAM'
         UNION ALL SELECT 'Cortes frescos', 'Matambre de cerdo', 'Precio de referencia por kilogramo en ARS.', 'CER-MAT-CER', 15100.00, 'KILOGRAM'
         UNION ALL SELECT 'Cortes frescos', 'Pulpa de pernil de cerdo', 'Precio de referencia por kilogramo en ARS.', 'CER-PUL-PER', 12800.00, 'KILOGRAM'
         UNION ALL SELECT 'Embutidos frescos', 'Chorizo puro cerdo', 'Precio de referencia por kilogramo en ARS.', 'EMB-CHO-PUR', 10500.00, 'KILOGRAM'
         UNION ALL SELECT 'Embutidos frescos', 'Chorizo parrillero', 'Precio de referencia por kilogramo en ARS.', 'EMB-CHO-PAR', 8900.00, 'KILOGRAM'
         UNION ALL SELECT 'Embutidos frescos', 'Morcilla', 'Precio de referencia por kilogramo en ARS.', 'EMB-MOR-CER', 7800.00, 'KILOGRAM'
         UNION ALL SELECT 'Salames y salamines', 'Salame tipo milán', 'Precio de referencia por kilogramo en ARS.', 'EMB-SAL-MIL', 21900.00, 'KILOGRAM'
         UNION ALL SELECT 'Salames y salamines', 'Salamín picado grueso', 'Precio de referencia por kilogramo en ARS.', 'EMB-SAL-PIC', 20500.00, 'KILOGRAM'
         UNION ALL SELECT 'Salames y salamines', 'Longaniza tipo calabresa', 'Precio de referencia por kilogramo en ARS.', 'EMB-LON-CAL', 22700.00, 'KILOGRAM'
         UNION ALL SELECT 'Salames y salamines', 'Salchichón primavera', 'Precio de referencia por kilogramo en ARS.', 'EMB-SAL-PRI', 14200.00, 'KILOGRAM'
         UNION ALL SELECT 'Jamones y paletas', 'Jamón cocido', 'Precio de referencia por kilogramo en ARS.', 'FIA-JAM-COC', 14500.00, 'KILOGRAM'
         UNION ALL SELECT 'Jamones y paletas', 'Paleta cocida', 'Precio de referencia por kilogramo en ARS.', 'FIA-PAL-COC', 11900.00, 'KILOGRAM'
         UNION ALL SELECT 'Jamones y paletas', 'Jamón crudo', 'Precio de referencia por kilogramo en ARS.', 'FIA-JAM-CRU', 28500.00, 'KILOGRAM'
         UNION ALL SELECT 'Jamones y paletas', 'Bondiola curada', 'Precio de referencia por kilogramo en ARS.', 'FIA-BON-CUR', 27900.00, 'KILOGRAM'
         UNION ALL SELECT 'Fiambres cocidos', 'Mortadela', 'Precio de referencia por kilogramo en ARS.', 'FIA-MOR-COC', 9900.00, 'KILOGRAM'
         UNION ALL SELECT 'Fiambres cocidos', 'Fiambre de cerdo feteado 200 g', 'Paquete cerrado; precio de referencia en ARS.', 'FIA-FET-CER200', 3100.00, 'UNIT'
         UNION ALL SELECT 'Pancetas y ahumados', 'Panceta ahumada', 'Precio de referencia por kilogramo en ARS.', 'FIA-PAN-AHU', 18700.00, 'KILOGRAM'
         UNION ALL SELECT 'Pancetas y ahumados', 'Panceta salada', 'Precio de referencia por kilogramo en ARS.', 'FIA-PAN-SAL', 15300.00, 'KILOGRAM'
         UNION ALL SELECT 'Pancetas y ahumados', 'Tocino de cerdo', 'Precio de referencia por kilogramo en ARS.', 'FIA-TOC-CER', 9200.00, 'KILOGRAM'
         UNION ALL SELECT 'Quesos', 'Queso barra', 'Precio de referencia por kilogramo en ARS.', 'QUE-BAR', 13900.00, 'KILOGRAM'
         UNION ALL SELECT 'Quesos', 'Queso cremoso', 'Precio de referencia por kilogramo en ARS.', 'QUE-CRE', 12600.00, 'KILOGRAM'
         UNION ALL SELECT 'Quesos', 'Queso pategrás', 'Precio de referencia por kilogramo en ARS.', 'QUE-PAT', 21900.00, 'KILOGRAM'
         UNION ALL SELECT 'Congelados', 'Medallones de cerdo x4', 'Paquete congelado; precio de referencia en ARS.', 'CON-MED-CER4', 4800.00, 'PACK'
         UNION ALL SELECT 'Congelados', 'Salchichas tipo viena x6', 'Paquete congelado; precio de referencia en ARS.', 'CON-SAL-VIE6', 2900.00, 'PACK'
         UNION ALL SELECT 'Aceitunas y encurtidos', 'Aceitunas verdes', 'Precio de referencia por kilogramo en ARS.', 'ACO-ACE-VER', 9500.00, 'KILOGRAM'
         UNION ALL SELECT 'Aceitunas y encurtidos', 'Pepinillos en vinagre', 'Frasco 360 g; precio de referencia en ARS.', 'ACO-PEP-VIN360', 3900.00, 'UNIT'
         UNION ALL SELECT 'Aderezos y complementos', 'Mostaza tipo Dijon 250 g', 'Precio de referencia en ARS.', 'ACO-MOS-250', 2400.00, 'UNIT'
         UNION ALL SELECT 'Aderezos y complementos', 'Salsa chimichurri 250 g', 'Precio de referencia en ARS.', 'ACO-CHI-250', 2600.00, 'UNIT'
         UNION ALL SELECT 'Aderezos y complementos', 'Tapas para empanadas x12', 'Paquete refrigerado; precio de referencia en ARS.', 'ACO-TAP-EMP12', 2200.00, 'PACK'
     ) AS product
         JOIN (
             SELECT name, MIN(id) AS id
             FROM categories
             WHERE parent_category_id IS NOT NULL
             GROUP BY name
         ) category ON category.name = product.category_name
WHERE NOT EXISTS (
    SELECT 1 FROM products existing WHERE existing.sku = product.sku
);

INSERT INTO trucks (license_plate, driver_id, is_active)
SELECT seed.license_plate, NULL, TRUE
FROM (
         SELECT 'DEMO-CH-001' AS license_plate
         UNION ALL SELECT 'DEMO-CH-002'
         UNION ALL SELECT 'DEMO-CH-003'
         UNION ALL SELECT 'DEMO-CH-004'
     ) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM trucks existing WHERE existing.license_plate = seed.license_plate
);

INSERT INTO customers (business_name, name, phone, email, is_active)
SELECT seed.business_name, 'Cliente de prueba', NULL, NULL, TRUE
FROM (
         SELECT 'Almacen Centro Demo' AS business_name
         UNION ALL SELECT 'Kiosco San Martin Demo'
         UNION ALL SELECT 'Autoservicio La Termal Demo'
         UNION ALL SELECT 'Despensa Mitre Demo'
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo'
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo'
         UNION ALL SELECT 'Almacen Nala Demo'
         UNION ALL SELECT 'Autoservicio Ensanche Norte Demo'
     ) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM customers existing WHERE existing.business_name = seed.business_name
);

INSERT INTO customer_addresses (customer_id, label, street_address, city, is_default)
SELECT customer.id, address.label, address.street_address, 'Presidencia Roque Sáenz Peña, Chaco', TRUE
FROM (
         SELECT 'Almacen Centro Demo' AS business_name, 'Local' AS label, 'Calle 12 (direccion ficticia DEMO)' AS street_address
         UNION ALL SELECT 'Kiosco San Martin Demo', 'Local', 'Calle 10 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Autoservicio La Termal Demo', 'Local', 'Calle 14 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Despensa Mitre Demo', 'Local', 'Calle 8 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Minimercado Ensanche Sur Demo', 'Local', 'Calle 28 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Kiosco Barrio Obrero Demo', 'Local', 'Calle 33 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Almacen Nala Demo', 'Local', 'Calle 51 (direccion ficticia DEMO)'
         UNION ALL SELECT 'Autoservicio Ensanche Norte Demo', 'Local', 'Calle 19 (direccion ficticia DEMO)'
     ) AS address
         JOIN (
             SELECT business_name, MIN(id) AS id
             FROM customers
             GROUP BY business_name
         ) customer ON customer.business_name = address.business_name
WHERE NOT EXISTS (
    SELECT 1 FROM customer_addresses existing
    WHERE existing.customer_id = customer.id
      AND existing.street_address = address.street_address
);
