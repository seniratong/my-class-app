CREATE DATABASE IF NOT EXISTS `__DATABASE__` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `__DATABASE__`;

CREATE TABLE IF NOT EXISTS rate_snapshots (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    update_date DATE NOT NULL UNIQUE,
    source_url VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS currency_rates (
    snapshot_id BIGINT UNSIGNED NOT NULL,
    position INT UNSIGNED NOT NULL,
    currency_name VARCHAR(80) NOT NULL,
    quote DECIMAL(20,8) NOT NULL,
    quote_unit INT NOT NULL DEFAULT 100,
    PRIMARY KEY (snapshot_id, currency_name),
    UNIQUE KEY snapshot_position (snapshot_id, position),
    CONSTRAINT positive_quote CHECK (quote > 0),
    CONSTRAINT per_hundred CHECK (quote_unit = 100),
    FOREIGN KEY (snapshot_id) REFERENCES rate_snapshots(id)
);
