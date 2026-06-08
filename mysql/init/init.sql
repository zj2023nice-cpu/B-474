-- Database initialization
-- Tables are created by Spring Boot (Hibernate) with ddl-auto: update

-- Note: User passwords are now stored as hashes with salt
-- The users table will have the following structure (managed by Hibernate):
-- - id: BIGINT (primary key, auto-increment)
-- - username: VARCHAR(255) (unique, not null)
-- - password: VARCHAR(255) (not null, stored as SHA-256 hash with salt)
-- - salt: VARCHAR(255) (used for password hashing)
-- - role: VARCHAR(255) (not null)
-- - name: VARCHAR(255)

-- Password security implementation:
-- 1. Frontend: Password is hashed with SHA-256 before transmission
-- 2. Backend: Received hash is hashed again with a unique salt per user
-- 3. Storage: Only the final hash and salt are stored in the database
-- 4. Verification: Same hashing process is applied and compared to stored hash
