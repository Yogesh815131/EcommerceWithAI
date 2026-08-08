-- Table exists now so the schema is ready, but there is deliberately no
-- create/list endpoint yet — a review requires proof of purchase (order_id),
-- and Order Service doesn't exist yet. Wired up once Order Service ships.
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (product_id, user_id, order_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
