CREATE TABLE stock_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    notified_at TIMESTAMP,
    UNIQUE (user_id, product_id)
);

CREATE INDEX idx_stock_alerts_product_id ON stock_alerts(product_id);