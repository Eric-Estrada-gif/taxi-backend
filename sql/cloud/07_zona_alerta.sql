-- Zonas de alerta para el conductor y el punto seguro del viaje.
-- Se puede correr más de una vez.

CREATE TABLE IF NOT EXISTS zona_alerta (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(120) NOT NULL,
    lat           DECIMAL(10,7) NOT NULL,
    lng           DECIMAL(10,7) NOT NULL,
    radio_metros  INT NOT NULL,
    activa        BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'viaje' AND column_name = 'origen_alerta') = 0,
    'ALTER TABLE viaje
        ADD COLUMN origen_alerta BOOLEAN NOT NULL DEFAULT FALSE,
        ADD COLUMN destino_alerta BOOLEAN NOT NULL DEFAULT FALSE,
        ADD COLUMN recojo_lat DECIMAL(10,7) NULL,
        ADD COLUMN recojo_lng DECIMAL(10,7) NULL,
        ADD COLUMN bajada_lat DECIMAL(10,7) NULL,
        ADD COLUMN bajada_lng DECIMAL(10,7) NULL,
        ADD COLUMN condicion_estado VARCHAR(20) NOT NULL DEFAULT ''NINGUNA'',
        ADD COLUMN condicion_conductor_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO zona_alerta (nombre, lat, lng, radio_metros, activa)
SELECT 'Av. Perú', -12.0315000, -77.0680000, 280, 1
WHERE NOT EXISTS (SELECT 1 FROM zona_alerta WHERE nombre = 'Av. Perú');

INSERT INTO zona_alerta (nombre, lat, lng, radio_metros, activa)
SELECT 'Plaza Norte', -12.0060300, -77.0592200, 250, 1
WHERE NOT EXISTS (SELECT 1 FROM zona_alerta WHERE nombre = 'Plaza Norte');

INSERT INTO zona_alerta (nombre, lat, lng, radio_metros, activa)
SELECT 'Jr. José María Barreto', -12.1102000, -77.0164000, 280, 1
WHERE NOT EXISTS (SELECT 1 FROM zona_alerta WHERE nombre = 'Jr. José María Barreto');
