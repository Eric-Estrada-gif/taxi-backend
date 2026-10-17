-- Borra usuarios, viajes y lo relacionado. Deja las tarifas.
-- Railway → MySQL → Query → pegar y Run.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM mensajes_viaje;
DELETE FROM calificacion;
DELETE FROM pago;
DELETE FROM ubicacion;
DELETE FROM dispositivo_push;
DELETE FROM metodo_pago;
DELETE FROM viaje;
UPDATE vehiculo SET conductor_id = NULL;
UPDATE conductor SET vehiculo_id = NULL;
DELETE FROM conductor;
DELETE FROM vehiculo;
DELETE FROM usuario;

SET FOREIGN_KEY_CHECKS = 1;
