ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(32) NULL AFTER delivered_at,
    ADD COLUMN cash_amount DECIMAL(12, 2) NULL AFTER payment_method,
    ADD COLUMN transfer_amount DECIMAL(12, 2) NULL AFTER cash_amount;
