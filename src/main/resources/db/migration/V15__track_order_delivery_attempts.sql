CREATE TABLE order_delivery_attempts
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_order_id BIGINT       NOT NULL,
    attempt_number    INT          NOT NULL,
    truck_id          BIGINT       NULL,
    delivery_date     DATE         NOT NULL,
    status            VARCHAR(20)  NOT NULL CHECK (status IN ('IN_PROGRESS', 'FAILED', 'DELIVERED')),
    failure_reason    VARCHAR(255) NULL,
    idempotency_key   VARCHAR(100) NULL UNIQUE,
    attempted_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at      DATETIME     NULL,
    created_by        BIGINT       NULL,
    CONSTRAINT uq_delivery_attempt_order_number UNIQUE (delivery_order_id, attempt_number),
    CONSTRAINT fk_delivery_attempt_order FOREIGN KEY (delivery_order_id) REFERENCES orders (id),
    CONSTRAINT fk_delivery_attempt_truck FOREIGN KEY (truck_id) REFERENCES trucks (id),
    CONSTRAINT fk_delivery_attempt_created_by FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_delivery_attempt_order_status
    ON order_delivery_attempts (delivery_order_id, status, attempt_number);

ALTER TABLE truck_stock_movements
    ADD COLUMN delivery_attempt_id BIGINT NULL AFTER delivery_order_id,
    ADD CONSTRAINT fk_tsm_delivery_attempt FOREIGN KEY (delivery_attempt_id) REFERENCES order_delivery_attempts (id);

CREATE INDEX idx_tsm_delivery_attempt_type
    ON truck_stock_movements (delivery_attempt_id, movement_type);
