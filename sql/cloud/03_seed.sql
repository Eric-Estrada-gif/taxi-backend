-- Solo 2 tarifas: la app bloquea Executive VIP (id 3 no existe a propósito).
USE taxi_app;

INSERT INTO tarifa (id, nombre, tarifa_base, precio_km, precio_minuto, vigente_desde, activo)
VALUES
    (1, 'Economico', 5.00, 1.20, 0.15, CURDATE(), TRUE),
    (2, 'Comfort', 6.50, 1.50, 0.20, CURDATE(), TRUE)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    tarifa_base = VALUES(tarifa_base),
    precio_km = VALUES(precio_km),
    precio_minuto = VALUES(precio_minuto),
    activo = TRUE;
