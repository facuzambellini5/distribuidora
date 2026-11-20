ALTER TABLE products
    ADD COLUMN unit VARCHAR(20) NOT NULL DEFAULT 'UNIT'
        CHECK (unit IN ('UNIT', 'KILOGRAM', 'GRAM', 'LITER', 'BOX', 'PACK'))
        AFTER unit_price;
