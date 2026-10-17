-- Habilita el tercer tipo de taxi (Executive VIP).
-- Railway → MySQL → Query → pegar y Run.

INSERT INTO tarifa (id, nombre, tarifa_base, precio_km, precio_minuto, vigente_desde, activo)
VALUES (3, 'Executive VIP', 9.00, 2.10, 0.30, CURDATE(), TRUE)
ON DUPLICATE KEY UPDATE
    nombre = 'Executive VIP',
    tarifa_base = 9.00,
    precio_km = 2.10,
    precio_minuto = 0.30,
    activo = TRUE;

ALTER TABLE tarifa AUTO_INCREMENT = 4;
