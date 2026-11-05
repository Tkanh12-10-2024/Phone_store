INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_STAFF');

-- Password mặc định: "admin123" (BCrypt hashed)
INSERT INTO users (username, password, status)
VALUES ('admin', '$2a$10$e81ZgN30D9E1yV3vO/4/3e8N520x41.H1g8G0G5G5G5G5G5G5G5G5', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

INSERT INTO employees (employee_code, full_name, email, phone, position, salary, status, user_id)
VALUES ('NV001', 'Nguyễn Văn Admin', 'admin@phonestore.com', '0901234567', 'Quản lý', 15000000, 'ACTIVE', 1);