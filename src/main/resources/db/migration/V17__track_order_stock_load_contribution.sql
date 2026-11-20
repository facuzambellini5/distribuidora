ALTER TABLE order_stock_loads
    ADD COLUMN load_contribution INT NOT NULL DEFAULT 0 AFTER quantity;

-- Previous reservations always added their entire quantity to delivery_stock.
UPDATE order_stock_loads
SET load_contribution = quantity;
