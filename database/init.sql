-- NOVA Dance Studio CRM Database Initialization Script
-- PostgreSQL Database Setup

-- Create database (run this as postgres superuser)
-- CREATE DATABASE nova_crm;

-- Create user and grant permissions
-- CREATE USER nova_user WITH PASSWORD 'nova_password';
-- GRANT ALL PRIVILEGES ON DATABASE nova_crm TO nova_user;

-- Connect to nova_crm database and run the following:
-- \c nova_crm;

-- Grant schema permissions
GRANT ALL ON SCHEMA public TO nova_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO nova_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO nova_user;

-- The application will automatically create tables using Hibernate DDL
-- with the 'update' strategy in production profile

-- Optional: Create indexes for better performance (run after first application start)
-- CREATE INDEX IF NOT EXISTS idx_students_email ON students(email);
-- CREATE INDEX IF NOT EXISTS idx_students_phone ON students(phone);
-- CREATE INDEX IF NOT EXISTS idx_teachers_email ON teachers(email);
-- CREATE INDEX IF NOT EXISTS idx_payments_student_id ON payments(student_id);
-- CREATE INDEX IF NOT EXISTS idx_payments_class_id ON payments(class_id);
-- CREATE INDEX IF NOT EXISTS idx_payments_month ON payments(month);
-- CREATE INDEX IF NOT EXISTS idx_payments_payment_date ON payments(payment_date);
