-- RapiTrip / taxi_app — esquema para nube (MySQL 8)
-- Idempotente en tablas nuevas. Ejecutar UNA vez en una base vacía.
-- Luego 02_procedures.sql y 03_seed.sql

CREATE DATABASE IF NOT EXISTS taxi_app
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE taxi_app;

CREATE TABLE IF NOT EXISTS usuario (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    google_id       VARCHAR(255) NULL,
    telefono        VARCHAR(20),
    rol             ENUM('RIDER','CONDUCTOR','ADMIN') NOT NULL DEFAULT 'RIDER',
    foto_perfil     VARCHAR(512),
    fecha_registro  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT uq_usuario_google UNIQUE (google_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS vehiculo (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    conductor_id        BIGINT NULL,
    placa               VARCHAR(10) NOT NULL,
    marca               VARCHAR(50) NOT NULL,
    modelo              VARCHAR(50) NOT NULL,
    anio                INT,
    color               VARCHAR(30),
    soat_vigente_hasta  DATE,
    CONSTRAINT uq_vehiculo_placa UNIQUE (placa)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS conductor (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id                  BIGINT NOT NULL,
    numero_licencia             VARCHAR(50) NOT NULL,
    fecha_vencimiento_licencia  DATE,
    estado                      ENUM('DISPONIBLE','OCUPADO','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
    calificacion_promedio       DECIMAL(3,2) DEFAULT 0.00,
    vehiculo_id                 BIGINT NULL,
    CONSTRAINT uq_conductor_usuario UNIQUE (usuario_id),
    CONSTRAINT uq_conductor_vehiculo UNIQUE (vehiculo_id),
    CONSTRAINT fk_conductor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_conductor_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo(id) ON DELETE SET NULL
) ENGINE=InnoDB;

ALTER TABLE vehiculo
    ADD CONSTRAINT fk_vehiculo_conductor FOREIGN KEY (conductor_id) REFERENCES conductor(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS tarifa (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(50) NOT NULL,
    tarifa_base     DECIMAL(6,2) NOT NULL,
    precio_km       DECIMAL(6,2) NOT NULL,
    precio_minuto   DECIMAL(6,2) NOT NULL DEFAULT 0.00,
    vigente_desde   DATE NOT NULL,
    activo          BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS viaje (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    rider_id            BIGINT NOT NULL,
    conductor_id        BIGINT NULL,
    tarifa_id           BIGINT NOT NULL,
    origen_direccion    VARCHAR(255) NOT NULL,
    origen_lat          DECIMAL(10,7) NOT NULL,
    origen_lng          DECIMAL(10,7) NOT NULL,
    destino_direccion   VARCHAR(255) NOT NULL,
    destino_lat         DECIMAL(10,7) NOT NULL,
    destino_lng         DECIMAL(10,7) NOT NULL,
    estado              ENUM('SOLICITADO','ACEPTADO','EN_CURSO','FINALIZADO','CANCELADO') NOT NULL DEFAULT 'SOLICITADO',
    monto_total         DECIMAL(8,2),
    fecha_solicitud     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio        DATETIME NULL,
    fecha_fin           DATETIME NULL,
    CONSTRAINT fk_viaje_rider FOREIGN KEY (rider_id) REFERENCES usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_viaje_conductor FOREIGN KEY (conductor_id) REFERENCES conductor(id) ON DELETE SET NULL,
    CONSTRAINT fk_viaje_tarifa FOREIGN KEY (tarifa_id) REFERENCES tarifa(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ubicacion (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    viaje_id    BIGINT NOT NULL,
    lat         DECIMAL(10,7) NOT NULL,
    lng         DECIMAL(10,7) NOT NULL,
    timestamp   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ubicacion_viaje FOREIGN KEY (viaje_id) REFERENCES viaje(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_ubicacion_viaje_timestamp ON ubicacion(viaje_id, timestamp);
CREATE INDEX idx_viaje_rider ON viaje(rider_id, fecha_solicitud);
CREATE INDEX idx_viaje_conductor ON viaje(conductor_id, fecha_solicitud);
CREATE INDEX idx_viaje_estado ON viaje(estado);

CREATE TABLE IF NOT EXISTS pago (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    viaje_id        BIGINT NOT NULL,
    metodo          ENUM('EFECTIVO','TARJETA','BILLETERA_DIGITAL','VIP') NOT NULL,
    monto           DECIMAL(8,2) NOT NULL,
    estado          ENUM('PENDIENTE','COMPLETADO','FALLIDO','REEMBOLSADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pago_viaje UNIQUE (viaje_id),
    CONSTRAINT fk_pago_viaje FOREIGN KEY (viaje_id) REFERENCES viaje(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS calificacion (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    viaje_id        BIGINT NOT NULL,
    usuario_id      BIGINT NOT NULL,
    puntaje         TINYINT NOT NULL,
    comentario      VARCHAR(500),
    fecha           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calificacion_viaje FOREIGN KEY (viaje_id) REFERENCES viaje(id) ON DELETE CASCADE,
    CONSTRAINT fk_calificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE,
    CONSTRAINT chk_puntaje CHECK (puntaje BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS metodo_pago (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT NOT NULL,
    tipo            ENUM('TARJETA','BILLETERA_DIGITAL') NOT NULL,
    detalle         VARCHAR(100) NOT NULL,
    predeterminado  BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_metodo_pago_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mensajes_viaje (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    viaje_id        BIGINT NOT NULL,
    remitente_tipo  VARCHAR(20) NOT NULL,
    mensaje         TEXT NOT NULL,
    fecha_envio     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_viaje_mensaje FOREIGN KEY (viaje_id) REFERENCES viaje(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS dispositivo_push (
    usuario_id   BIGINT NOT NULL,
    token        VARCHAR(512) NOT NULL,
    actualizado  DATETIME NOT NULL,
    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_dispositivo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB;
