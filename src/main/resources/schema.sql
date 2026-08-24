-- Database schema for Ceramics Manufacturing Pipeline (ceramics_pipeline)

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'WORKER',
    telegram_chat_id VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sequence_order INT NOT NULL,
    default_duration_hours INT NOT NULL DEFAULT 24
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(30) NOT NULL UNIQUE,
    customer_name VARCHAR(150),
    raw_description TEXT NOT NULL,
    quantity INT NOT NULL,
    deadline_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_extractions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_name VARCHAR(200),
    pattern VARCHAR(150),
    height_cm DECIMAL(6,2),
    glaze_type VARCHAR(100),
    estimated_clay_kg DECIMAL(10,2),
    firing_temp_celsius INT,
    firing_duration_hours INT,
    priority_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    raw_ai_json JSON NOT NULL,
    ai_model VARCHAR(50),
    confidence_note VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_extractions_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_code VARCHAR(30) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    current_stage_id INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_batches_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_batches_current_stage FOREIGN KEY (current_stage_id) REFERENCES stages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch_stage_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    stage_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at DATETIME,
    completed_at DATETIME,
    performed_by BIGINT,
    note VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_batch FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_stage FOREIGN KEY (stage_id) REFERENCES stages(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_performed_by FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_batch_stage UNIQUE (batch_id, stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qc_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    total_checked INT NOT NULL,
    passed_count INT NOT NULL,
    failed_count INT NOT NULL,
    defect_type VARCHAR(150),
    defect_note VARCHAR(500),
    is_critical BOOLEAN NOT NULL DEFAULT FALSE,
    checked_by BIGINT,
    checked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_qc_batch FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_qc_checked_by FOREIGN KEY (checked_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT,
    alert_type VARCHAR(20) NOT NULL DEFAULT 'INFO',
    channel VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_message_id VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME,
    CONSTRAINT fk_alerts_batch FOREIGN KEY (batch_id) REFERENCES batches(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
