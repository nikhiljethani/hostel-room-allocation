-- =====================================================================
--  Hostel Room Allocation System  -  MySQL 8.x database script
--  Run this whole file once in MySQL Workbench.
--  WARNING: it DROPS and re-creates the four tables, so running it
--  again resets all data back to the demo data below.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS hostel_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hostel_db;

DROP TABLE IF EXISTS allocations;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS admins;

-- ---------------------------------------------------------------------
-- admins : login accounts (password is stored as a SHA-256 hash)
-- ---------------------------------------------------------------------
CREATE TABLE admins (
    admin_id   INT          NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    PRIMARY KEY (admin_id),
    UNIQUE KEY uq_admins_username (username)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- students
-- ---------------------------------------------------------------------
CREATE TABLE students (
    student_id    INT          NOT NULL AUTO_INCREMENT,
    enrollment_no VARCHAR(30)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    gender        VARCHAR(10),
    course        VARCHAR(50),
    `year`        INT,
    phone         VARCHAR(15),
    email         VARCHAR(100),
    address       VARCHAR(200),
    PRIMARY KEY (student_id),
    UNIQUE KEY uq_students_enrollment (enrollment_no),
    CONSTRAINT chk_students_year CHECK (`year` IS NULL OR `year` BETWEEN 1 AND 6)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- rooms : NOTE there are NO occupied / available / status columns.
--         Those are calculated from the allocations table by queries.
-- ---------------------------------------------------------------------
CREATE TABLE rooms (
    room_id     INT         NOT NULL AUTO_INCREMENT,
    room_number VARCHAR(10) NOT NULL,
    block       VARCHAR(20),
    `floor`     INT,
    room_type   VARCHAR(30),
    capacity    INT         NOT NULL,
    PRIMARY KEY (room_id),
    UNIQUE KEY uq_rooms_number (room_number),
    CONSTRAINT chk_rooms_capacity CHECK (capacity > 0),
    CONSTRAINT chk_rooms_floor CHECK (`floor` IS NULL OR `floor` >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- allocations : one row per allocation. status = ACTIVE or VACATED.
--               VACATED rows are kept as history (never deleted).
-- ---------------------------------------------------------------------
CREATE TABLE allocations (
    allocation_id   INT         NOT NULL AUTO_INCREMENT,
    student_id      INT         NOT NULL,
    room_id         INT         NOT NULL,
    allocation_date DATE        NOT NULL,
    vacate_date     DATE        NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (allocation_id),
    KEY idx_alloc_student_status (student_id, status),
    KEY idx_alloc_room_status (room_id, status),
    CONSTRAINT fk_alloc_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_alloc_room FOREIGN KEY (room_id)
        REFERENCES rooms (room_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_alloc_status CHECK (status IN ('ACTIVE', 'VACATED')),
    CONSTRAINT chk_alloc_dates CHECK (vacate_date IS NULL OR vacate_date >= allocation_date),
    CONSTRAINT chk_alloc_consistency CHECK (
        (status = 'ACTIVE'  AND vacate_date IS NULL) OR
        (status = 'VACATED' AND vacate_date IS NOT NULL))
) ENGINE = InnoDB;

-- =====================================================================
--  DEMO DATA
-- =====================================================================

-- Default login:  username = admin   password = admin123
INSERT INTO admins (username, `password`) VALUES ('admin', SHA2('admin123', 256));

INSERT INTO students (enrollment_no, name, gender, course, `year`, phone, email, address) VALUES
('BT2023001', 'Aarav Sharma',    'Male',   'B.Tech CSE', 2, '9876500001', 'aarav.sharma@example.com',    'Jaipur, Rajasthan'),
('BT2023002', 'Rohan Mehta',     'Male',   'B.Tech CSE', 2, '9876500002', 'rohan.mehta@example.com',     'Udaipur, Rajasthan'),
('BT2023003', 'Karan Singh',     'Male',   'B.Tech ECE', 2, '9876500003', 'karan.singh@example.com',     'Jodhpur, Rajasthan'),
('BT2024004', 'Priya Verma',     'Female', 'B.Tech IT',  1, '9876500004', 'priya.verma@example.com',     'Kota, Rajasthan'),
('BT2024005', 'Neha Gupta',      'Female', 'B.Tech IT',  1, '9876500005', 'neha.gupta@example.com',      'Ajmer, Rajasthan'),
('MC2024006', 'Ananya Joshi',    'Female', 'MCA',        1, '9876500006', 'ananya.joshi@example.com',    'Bikaner, Rajasthan'),
('BT2022007', 'Vikram Rathore',  'Male',   'B.Tech ME',  3, '9876500007', 'vikram.rathore@example.com',  'Alwar, Rajasthan'),
('BT2024008', 'Sneha Agarwal',   'Female', 'B.Tech CSE', 1, '9876500008', 'sneha.agarwal@example.com',   'Jaipur, Rajasthan'),
('BC2023009', 'Rahul Meena',     'Male',   'BCA',        2, '9876500009', 'rahul.meena@example.com',     'Sikar, Rajasthan'),
('MC2023010', 'Pooja Choudhary', 'Female', 'MCA',        2, '9876500010', 'pooja.choudhary@example.com', 'Bharatpur, Rajasthan');

INSERT INTO rooms (room_number, block, `floor`, room_type, capacity) VALUES
('101', 'A', 1, 'Four Sharing', 4),
('102', 'A', 1, 'Double',       2),
('103', 'A', 1, 'Double',       2),
('201', 'B', 2, 'Triple',       3),
('202', 'B', 2, 'Single',       1),
('203', 'B', 2, 'Four Sharing', 4);

-- Active allocations
--   Room 101 (cap 4): 3 active  -> AVAILABLE (1 bed free)
--   Room 102 (cap 2): 2 active  -> FULL
--   Room 201 (cap 3): 1 active  -> AVAILABLE (2 beds free)
--   Rooms 103, 202, 203         -> EMPTY
INSERT INTO allocations (student_id, room_id, allocation_date, vacate_date, status) VALUES
(1, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), NULL, 'ACTIVE'),
(2, 1, DATE_SUB(CURDATE(), INTERVAL 58 DAY), NULL, 'ACTIVE'),
(3, 1, DATE_SUB(CURDATE(), INTERVAL 45 DAY), NULL, 'ACTIVE'),
(4, 2, DATE_SUB(CURDATE(), INTERVAL 40 DAY), NULL, 'ACTIVE'),
(5, 2, DATE_SUB(CURDATE(), INTERVAL 40 DAY), NULL, 'ACTIVE'),
(6, 4, DATE_SUB(CURDATE(), INTERVAL 20 DAY), NULL, 'ACTIVE');

-- One vacated allocation (history): student 7 stayed in room 103 earlier
INSERT INTO allocations (student_id, room_id, allocation_date, vacate_date, status) VALUES
(7, 3, DATE_SUB(CURDATE(), INTERVAL 90 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'VACATED');

-- ---------------------------------------------------------------------
-- Quick check (same logic the Java application uses):
-- occupied = ACTIVE allocations, available = capacity - occupied
-- ---------------------------------------------------------------------
SELECT r.room_number, r.block, r.capacity,
       COUNT(a.allocation_id)                AS occupied,
       r.capacity - COUNT(a.allocation_id)   AS available,
       CASE WHEN COUNT(a.allocation_id) = 0          THEN 'EMPTY'
            WHEN COUNT(a.allocation_id) < r.capacity THEN 'AVAILABLE'
            ELSE 'FULL' END                  AS status
FROM rooms r
LEFT JOIN allocations a ON a.room_id = r.room_id AND a.status = 'ACTIVE'
GROUP BY r.room_id, r.room_number, r.block, r.capacity
ORDER BY r.room_number;
