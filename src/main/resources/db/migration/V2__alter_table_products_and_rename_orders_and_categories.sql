ALTER TABLE products
DROP COLUMN roast_level,
DROP COLUMN grind_type,
DROP COLUMN net_weight_grams,
DROP COLUMN photo_url;

RENAME TABLE delivery_orders TO orders;

RENAME TABLE product_categories TO categories;
