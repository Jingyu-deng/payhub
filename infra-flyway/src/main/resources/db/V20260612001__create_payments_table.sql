-- PayHub initial schema
-- V1: payments table — PaymentEntity.orderId is a plain string (no FK).

CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_gateway VARCHAR(30),
    transaction_id VARCHAR(128),
    gateway_response TEXT,
    notify_url VARCHAR(1024),
    check_pg_status_control_job_key VARCHAR(36),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
