-- Initial seed data for Ceramics Manufacturing Pipeline

INSERT INTO users (id, username, password_hash, full_name, role, telegram_chat_id, is_active)
VALUES 
(1, 'admin', '$2a$10$abcdefghijklmnopqrstuvwxyz012345', 'Quản lý xưởng', 'ADMIN', '123456789', TRUE),
(2, 'tho_tao_hinh', '$2a$10$abcdefghijklmnopqrstuvwxyz012345', 'Thợ Tạo Hình', 'WORKER', '987654321', TRUE),
(3, 'tho_qc', '$2a$10$abcdefghijklmnopqrstuvwxyz012345', 'Kiểm Định QC', 'WORKER', '555666777', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

INSERT INTO stages (id, code, name, sequence_order, default_duration_hours)
VALUES 
(1, 'FORMING', 'Tạo hình mộc', 1, 24),
(2, 'DRYING_TRIMMING', 'Phơi sấy & Sửa mộc', 2, 48),
(3, 'PAINTING', 'Vẽ họa tiết', 3, 36),
(4, 'GLAZING', 'Tráng men', 4, 12),
(5, 'FIRING', 'Vào lò nung', 5, 24),
(6, 'QC_PACKAGING', 'Kiểm định chất lượng (QC) & Đóng gói', 6, 12)
ON DUPLICATE KEY UPDATE name = VALUES(name), sequence_order = VALUES(sequence_order);

-- Sample Order
INSERT INTO orders (id, order_code, customer_name, raw_description, quantity, deadline_date, status, created_by)
VALUES 
(1, 'ORD-20260824-001', 'Công ty Bát Tràng Export', 'Đơn 200 Bình gốm họa tiết sen men lam cao 35cm, yêu cầu nung nhiệt độ cao 1280°C, hoàn thành trong 10 ngày', 200, DATE_ADD(CURRENT_DATE, INTERVAL 10 DAY), 'PROCESSING', 1)
ON DUPLICATE KEY UPDATE order_code = VALUES(order_code);

-- Sample AI Extraction
INSERT INTO ai_extractions (id, order_id, product_name, pattern, height_cm, glaze_type, estimated_clay_kg, firing_temp_celsius, firing_duration_hours, priority_level, raw_ai_json, ai_model, confidence_note)
VALUES 
(1, 1, 'Bình gốm họa tiết sen', 'Hoa sen men lam', 35.00, 'Men lam truyền thống', 300.00, 1280, 24, 'HIGH', 
'{"product_name": "Bình gốm họa tiết sen", "pattern": "Hoa sen men lam", "height_cm": 35.0, "glaze_type": "Men lam truyền thống", "quantity": 200, "estimated_clay_kg": 300.0, "firing_temp_celsius": 1280, "firing_duration_hours": 24, "priority_level": "HIGH", "deadline_days": 10, "confidence_note": "Trích xuất thành công từ mô tả đơn hàng"}', 
'OpenAI-GPT-4o', 'Trích xuất chính xác 100%')
ON DUPLICATE KEY UPDATE product_name = VALUES(product_name);

-- Sample Batch
INSERT INTO batches (id, batch_code, order_id, quantity, current_stage_id, status, priority_level, started_at)
VALUES 
(1, 'GOM-20260824-01', 1, 200, 1, 'IN_PROGRESS', 'HIGH', NOW())
ON DUPLICATE KEY UPDATE batch_code = VALUES(batch_code);

-- Sample Batch Stage History (6 stages)
INSERT INTO batch_stage_history (batch_id, stage_id, status, started_at, completed_at, performed_by, note)
VALUES 
(1, 1, 'IN_PROGRESS', NOW(), NULL, 2, 'Đang tiến hành nhào đất và chuốt hình mộc'),
(1, 2, 'PENDING', NULL, NULL, NULL, NULL),
(1, 3, 'PENDING', NULL, NULL, NULL, NULL),
(1, 4, 'PENDING', NULL, NULL, NULL, NULL),
(1, 5, 'PENDING', NULL, NULL, NULL, NULL),
(1, 6, 'PENDING', NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE status = VALUES(status);
