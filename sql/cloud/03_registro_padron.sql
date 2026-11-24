-- Padron simulado (DNI, licencia, SOAT) + verificacion y test de conducta.
-- Correr en MySQL local (taxi_app). No requiere Railway.

CREATE TABLE IF NOT EXISTS padron_dni (
    dni                 VARCHAR(8) PRIMARY KEY,
    nombres             VARCHAR(150) NOT NULL,
    fecha_caducidad     DATE NOT NULL,
    estado              ENUM('VIGENTE','POR_RENOVAR','VENCIDO') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS padron_licencia (
    dni                 VARCHAR(8) PRIMARY KEY,
    nombres             VARCHAR(150) NOT NULL,
    numero_licencia     VARCHAR(20) NOT NULL,
    categoria           VARCHAR(10) NOT NULL,
    fecha_vencimiento   DATE NOT NULL,
    estado              ENUM('VIGENTE','POR_RENOVAR','VENCIDA') NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS padron_soat (
    placa               VARCHAR(10) PRIMARY KEY,
    dni_titular         VARCHAR(8) NOT NULL,
    compania            VARCHAR(80) NOT NULL,
    fecha_inicio        DATE NOT NULL,
    fecha_fin           DATE NOT NULL,
    estado              ENUM('VIGENTE','POR_RENOVAR','VENCIDO','NO_REGISTRA') NOT NULL,
    marca               VARCHAR(40) NULL,
    modelo              VARCHAR(40) NULL,
    color               VARCHAR(30) NULL,
    clase               VARCHAR(10) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS verificacion_conductor (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conductor_id    BIGINT NULL,
    dni             VARCHAR(8) NOT NULL,
    placa           VARCHAR(10) NOT NULL,
    veredicto       ENUM('APROBADO','OBSERVADO','RECHAZADO') NOT NULL,
    mensaje         VARCHAR(500) NOT NULL,
    detalle_json    TEXT NOT NULL,
    fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS test_pregunta (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    rol         ENUM('RIDER','CONDUCTOR') NOT NULL,
    enunciado   VARCHAR(500) NOT NULL,
    opcion_a    VARCHAR(300) NOT NULL,
    opcion_b    VARCHAR(300) NOT NULL,
    opcion_c    VARCHAR(300) NOT NULL,
    opcion_d    VARCHAR(300) NOT NULL,
    correcta    CHAR(1) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS test_intento (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(150) NOT NULL,
    telefono        VARCHAR(20) NULL,
    rol             ENUM('RIDER','CONDUCTOR') NOT NULL,
    nro_intento     TINYINT NOT NULL,
    nota            TINYINT NOT NULL,
    porcentaje      DECIMAL(5,2) NOT NULL,
    aprobado        BOOLEAN NOT NULL,
    fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS test_bloqueo (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(150) NOT NULL,
    telefono        VARCHAR(20) NULL,
    bloqueado_hasta DATETIME NOT NULL,
    motivo          VARCHAR(255) NOT NULL,
    UNIQUE KEY uq_bloqueo_email (email)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS padron_antecedentes;
DELETE FROM padron_soat;
DELETE FROM padron_licencia;
DELETE FROM padron_dni;

-- Dos filas por situación en cada tabla. Por renovar = vigente con fecha dentro de 15 días.
INSERT INTO padron_dni (dni, nombres, fecha_caducidad, estado) VALUES
('41000001','ANA TORRES QUISPE','2029-12-31','VIGENTE'),
('41000002','LUIS RAMOS DIAZ','2029-06-15','VIGENTE'),
('41000011','ROSA GUTIERREZ SILVA','2026-11-28','POR_RENOVAR'),
('41000012','ELENA VEGA CASTRO','2026-11-25','POR_RENOVAR'),
('41000021','ANDRES QUISPE MAMA','2024-01-15','VENCIDO'),
('41000022','PATRICIA NUNEZ SALAS','2023-12-01','VENCIDO');

INSERT INTO padron_licencia (dni, nombres, numero_licencia, categoria, fecha_vencimiento, estado) VALUES
('41000001','ANA TORRES QUISPE','L-450001','A-IIb','2029-12-31','VIGENTE'),
('41000002','LUIS RAMOS DIAZ','L-450002','A-IIa','2029-06-15','VIGENTE'),
('41000011','ROSA GUTIERREZ SILVA','L-450011','A-IIa','2026-11-28','POR_RENOVAR'),
('41000012','ELENA VEGA CASTRO','L-450012','A-IIb','2026-11-25','POR_RENOVAR'),
('41000021','ANDRES QUISPE MAMA','L-450021','A-III','2024-08-12','VENCIDA'),
('41000022','PATRICIA NUNEZ SALAS','L-450022','A-IIb','2024-03-01','VENCIDA');

INSERT INTO padron_soat (placa, dni_titular, compania, fecha_inicio, fecha_fin, estado, marca, modelo, color, clase) VALUES
('AAA101','41000001','La Positiva','2026-01-01','2029-12-31','VIGENTE','Toyota','Yaris','Blanco','A-II'),
('AAA102','41000002','Rimac','2026-01-01','2029-06-15','VIGENTE','Hyundai','Accent','Gris','A-I'),
('AAA111','41000011','Pacifico','2026-01-01','2026-11-28','POR_RENOVAR','Kia','Rio','Negro','A-II'),
('AAA112','41000012','Mapfre','2026-01-01','2026-11-25','POR_RENOVAR','Nissan','Versa','Rojo','A-I'),
('AAA121','41000021','La Positiva','2024-01-01','2025-03-03','VENCIDO','Chevrolet','Sail','Azul','A-II'),
('AAA122','41000022','Rimac','2024-01-01','2025-09-09','VENCIDO','Volkswagen','Gol','Blanco','A-I');
