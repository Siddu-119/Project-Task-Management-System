-- Run this script manually in MySQL to set up the database

CREATE DATABASE IF NOT EXISTS project_management;
USE project_management;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'EMPLOYEE') NOT NULL
);

-- Projects table
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE
);

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'PENDING',
    deadline DATE,
    project_id BIGINT,
    assigned_to BIGINT,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL
);

-- Sample Users (passwords stored as plain text for demo purposes)
INSERT INTO users (name, email, password, role) VALUES
('Admin Manager', 'admin@example.com', 'admin123', 'ADMIN'),
('Alice Johnson', 'alice@example.com', 'alice123', 'EMPLOYEE'),
('Bob Smith', 'bob@example.com', 'bob123', 'EMPLOYEE'),
('Carol Williams', 'carol@example.com', 'carol123', 'EMPLOYEE'),
('David Brown', 'david@example.com', 'david123', 'EMPLOYEE');

-- Sample Projects
INSERT INTO projects (name, description, start_date, end_date) VALUES
('E-Commerce Platform', 'Build a full-featured online shopping platform with cart, checkout and payment integration.', '2025-01-01', '2025-06-30'),
('Mobile App Redesign', 'Redesign the existing mobile application for better UX and performance.', '2025-02-01', '2025-05-31'),
('Data Analytics Dashboard', 'Create a real-time analytics dashboard for business intelligence.', '2025-03-01', '2025-08-31');

-- Sample Tasks
INSERT INTO tasks (title, description, status, deadline, project_id, assigned_to) VALUES
('Setup project repository', 'Initialize Git repository and configure CI/CD pipeline', 'COMPLETED', '2025-01-10', 1, 2),
('Design database schema', 'Create ERD and implement MySQL database schema', 'COMPLETED', '2025-01-15', 1, 3),
('Build REST APIs', 'Develop Spring Boot REST APIs for product, cart and order management', 'IN_PROGRESS', '2025-02-28', 1, 2),
('Implement frontend', 'Build React.js frontend with responsive design', 'PENDING', '2025-03-31', 1, 4),
('UI/UX wireframes', 'Create wireframes and prototypes for new mobile design', 'COMPLETED', '2025-02-10', 2, 5),
('Implement new navigation', 'Rebuild navigation system based on approved wireframes', 'IN_PROGRESS', '2025-03-15', 2, 3),
('Performance optimization', 'Optimize app load time and reduce memory usage', 'PENDING', '2025-04-30', 2, 5),
('Data pipeline setup', 'Configure ETL pipeline for data ingestion', 'IN_PROGRESS', '2025-03-31', 3, 4),
('Dashboard charts', 'Implement interactive charts using D3.js or Chart.js', 'PENDING', '2025-05-15', 3, 2),
('User authentication module', 'Add role-based access control to dashboard', 'PENDING', '2025-04-15', 3, 3);
