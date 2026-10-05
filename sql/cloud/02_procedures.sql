USE taxi_app;

DROP PROCEDURE IF EXISTS sp_usuario_insertar;
DROP PROCEDURE IF EXISTS sp_usuario_insertar_google;
DROP PROCEDURE IF EXISTS sp_usuario_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_usuario_buscar_por_email;
DROP PROCEDURE IF EXISTS sp_usuario_buscar_por_google_id;
DROP PROCEDURE IF EXISTS sp_usuario_listar_por_rol;
DROP PROCEDURE IF EXISTS sp_usuario_actualizar;
DROP PROCEDURE IF EXISTS sp_usuario_desactivar;
DROP PROCEDURE IF EXISTS sp_tarifa_insertar;
DROP PROCEDURE IF EXISTS sp_tarifa_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_tarifa_listar_activas;
DROP PROCEDURE IF EXISTS sp_tarifa_desactivar;
DROP PROCEDURE IF EXISTS sp_conductor_insertar;
DROP PROCEDURE IF EXISTS sp_conductor_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_conductor_buscar_por_usuario;
DROP PROCEDURE IF EXISTS sp_conductor_listar_por_estado;
DROP PROCEDURE IF EXISTS sp_conductor_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_conductor_actualizar_calificacion;
DROP PROCEDURE IF EXISTS sp_vehiculo_insertar;
DROP PROCEDURE IF EXISTS sp_vehiculo_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_vehiculo_buscar_por_placa;
DROP PROCEDURE IF EXISTS sp_vehiculo_actualizar;
DROP PROCEDURE IF EXISTS sp_viaje_insertar;
DROP PROCEDURE IF EXISTS sp_viaje_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_viaje_listar_por_rider;
DROP PROCEDURE IF EXISTS sp_viaje_listar_por_conductor;
DROP PROCEDURE IF EXISTS sp_viaje_listar_solicitados;
DROP PROCEDURE IF EXISTS sp_viaje_aceptar;
DROP PROCEDURE IF EXISTS sp_viaje_iniciar;
DROP PROCEDURE IF EXISTS sp_viaje_finalizar;
DROP PROCEDURE IF EXISTS sp_viaje_cancelar;
DROP PROCEDURE IF EXISTS sp_viaje_historial_por_rider;
DROP PROCEDURE IF EXISTS sp_ubicacion_insertar;
DROP PROCEDURE IF EXISTS sp_ubicacion_listar_por_viaje;
DROP PROCEDURE IF EXISTS sp_ubicacion_obtener_ultima;
DROP PROCEDURE IF EXISTS sp_pago_insertar;
DROP PROCEDURE IF EXISTS sp_pago_buscar_por_id;
DROP PROCEDURE IF EXISTS sp_pago_buscar_por_viaje;
DROP PROCEDURE IF EXISTS sp_pago_actualizar_estado;
DROP PROCEDURE IF EXISTS sp_calificacion_insertar;
DROP PROCEDURE IF EXISTS sp_calificacion_listar_por_viaje;
DROP PROCEDURE IF EXISTS sp_calificacion_listar_recibidas_por_conductor;
DROP PROCEDURE IF EXISTS sp_metodo_pago_insertar;
DROP PROCEDURE IF EXISTS sp_metodo_pago_listar_por_usuario;
DROP PROCEDURE IF EXISTS sp_metodo_pago_eliminar;

DELIMITER //

CREATE PROCEDURE sp_usuario_insertar(
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_email VARCHAR(150),
    IN p_password VARCHAR(255),
    IN p_telefono VARCHAR(20),
    IN p_rol VARCHAR(20),
    OUT p_id BIGINT
)
BEGIN
    INSERT INTO usuario (nombre, apellido, email, password, telefono, rol, fecha_registro, activo)
    VALUES (p_nombre, p_apellido, p_email, p_password, p_telefono, p_rol, NOW(), TRUE);
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_usuario_insertar_google(
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_email VARCHAR(150),
    IN p_google_id VARCHAR(255),
    IN p_foto_perfil VARCHAR(512),
    IN p_rol VARCHAR(20),
    OUT p_id BIGINT
)
BEGIN
    INSERT INTO usuario (nombre, apellido, email, password, telefono, rol, foto_perfil, google_id, fecha_registro, activo)
    VALUES (IFNULL(p_nombre, ''), IFNULL(p_apellido, ''), p_email, CONCAT('{google}', p_google_id),
            NULL, p_rol, p_foto_perfil, p_google_id, NOW(), TRUE);
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_usuario_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, nombre, apellido, email, password, telefono, rol, foto_perfil, fecha_registro, activo, google_id
    FROM usuario WHERE id = p_id;
END //

CREATE PROCEDURE sp_usuario_buscar_por_email(IN p_email VARCHAR(150))
BEGIN
    SELECT id, nombre, apellido, email, password, telefono, rol, foto_perfil, fecha_registro, activo, google_id
    FROM usuario WHERE email = p_email;
END //

CREATE PROCEDURE sp_usuario_buscar_por_google_id(IN p_google_id VARCHAR(255))
BEGIN
    SELECT id, nombre, apellido, email, password, telefono, rol, foto_perfil, fecha_registro, activo, google_id
    FROM usuario WHERE google_id = p_google_id;
END //

CREATE PROCEDURE sp_usuario_listar_por_rol(IN p_rol VARCHAR(20))
BEGIN
    SELECT id, nombre, apellido, email, password, telefono, rol, foto_perfil, fecha_registro, activo, google_id
    FROM usuario WHERE rol = p_rol AND activo = TRUE;
END //

CREATE PROCEDURE sp_usuario_actualizar(
    IN p_id BIGINT,
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_telefono VARCHAR(20),
    IN p_foto_perfil VARCHAR(512)
)
BEGIN
    UPDATE usuario
    SET nombre = p_nombre, apellido = p_apellido, telefono = p_telefono, foto_perfil = p_foto_perfil
    WHERE id = p_id;
END //

CREATE PROCEDURE sp_usuario_desactivar(IN p_id BIGINT)
BEGIN
    UPDATE usuario SET activo = FALSE WHERE id = p_id;
END //

CREATE PROCEDURE sp_tarifa_insertar(
    IN p_nombre VARCHAR(50),
    IN p_tarifa_base DECIMAL(6,2),
    IN p_precio_km DECIMAL(6,2),
    IN p_precio_minuto DECIMAL(6,2),
    IN p_vigente_desde DATE,
    OUT p_id BIGINT
)
BEGIN
    INSERT INTO tarifa (nombre, tarifa_base, precio_km, precio_minuto, vigente_desde, activo)
    VALUES (p_nombre, p_tarifa_base, p_precio_km, p_precio_minuto, p_vigente_desde, TRUE);
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_tarifa_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, nombre, tarifa_base, precio_km, precio_minuto, vigente_desde, activo
    FROM tarifa WHERE id = p_id;
END //

CREATE PROCEDURE sp_tarifa_listar_activas()
BEGIN
    SELECT id, nombre, tarifa_base, precio_km, precio_minuto, vigente_desde, activo
    FROM tarifa WHERE activo = TRUE;
END //

CREATE PROCEDURE sp_tarifa_desactivar(IN p_id BIGINT)
BEGIN
    UPDATE tarifa SET activo = FALSE WHERE id = p_id;
END //

CREATE PROCEDURE sp_conductor_insertar(
    IN p_usuario_id BIGINT,
    IN p_numero_licencia VARCHAR(50),
    IN p_fecha_vencimiento_licencia DATE,
    IN p_vehiculo_id BIGINT,
    OUT p_id BIGINT
)
BEGIN
    INSERT INTO conductor (usuario_id, numero_licencia, fecha_vencimiento_licencia, estado, calificacion_promedio, vehiculo_id)
    VALUES (p_usuario_id, p_numero_licencia, p_fecha_vencimiento_licencia, 'OFFLINE', 0.00, p_vehiculo_id);
    SET p_id = LAST_INSERT_ID();
    UPDATE vehiculo SET conductor_id = p_id WHERE id = p_vehiculo_id;
END //

CREATE PROCEDURE sp_conductor_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, usuario_id, numero_licencia, fecha_vencimiento_licencia, estado, calificacion_promedio, vehiculo_id
    FROM conductor WHERE id = p_id;
END //

CREATE PROCEDURE sp_conductor_buscar_por_usuario(IN p_usuario_id BIGINT)
BEGIN
    SELECT id, usuario_id, numero_licencia, fecha_vencimiento_licencia, estado, calificacion_promedio, vehiculo_id
    FROM conductor WHERE usuario_id = p_usuario_id;
END //

CREATE PROCEDURE sp_conductor_listar_por_estado(IN p_estado VARCHAR(20))
BEGIN
    SELECT id, usuario_id, numero_licencia, fecha_vencimiento_licencia, estado, calificacion_promedio, vehiculo_id
    FROM conductor WHERE estado = p_estado;
END //

CREATE PROCEDURE sp_conductor_actualizar_estado(IN p_id BIGINT, IN p_estado VARCHAR(20))
BEGIN
    UPDATE conductor SET estado = p_estado WHERE id = p_id;
END //

CREATE PROCEDURE sp_conductor_actualizar_calificacion(IN p_id BIGINT, IN p_calificacion_promedio DECIMAL(3,2))
BEGIN
    UPDATE conductor SET calificacion_promedio = p_calificacion_promedio WHERE id = p_id;
END //

CREATE PROCEDURE sp_vehiculo_insertar(
    IN p_placa VARCHAR(10),
    IN p_marca VARCHAR(50),
    IN p_modelo VARCHAR(50),
    IN p_anio INT,
    IN p_color VARCHAR(30),
    IN p_soat_vigente_hasta DATE,
    OUT p_id BIGINT
)
BEGIN
    INSERT INTO vehiculo (placa, marca, modelo, anio, color, soat_vigente_hasta)
    VALUES (p_placa, p_marca, p_modelo, p_anio, p_color, p_soat_vigente_hasta);
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_vehiculo_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, conductor_id, placa, marca, modelo, anio, color, soat_vigente_hasta FROM vehiculo WHERE id = p_id;
END //

CREATE PROCEDURE sp_vehiculo_buscar_por_placa(IN p_placa VARCHAR(10))
BEGIN
    SELECT id, conductor_id, placa, marca, modelo, anio, color, soat_vigente_hasta FROM vehiculo WHERE placa = p_placa;
END //

CREATE PROCEDURE sp_vehiculo_actualizar(
    IN p_id BIGINT, IN p_marca VARCHAR(50), IN p_modelo VARCHAR(50),
    IN p_anio INT, IN p_color VARCHAR(30), IN p_soat_vigente_hasta DATE
)
BEGIN
    UPDATE vehiculo SET marca = p_marca, modelo = p_modelo, anio = p_anio,
        color = p_color, soat_vigente_hasta = p_soat_vigente_hasta WHERE id = p_id;
END //

CREATE PROCEDURE sp_viaje_insertar(
    IN p_rider_id BIGINT, IN p_tarifa_id BIGINT,
    IN p_origen_direccion VARCHAR(255), IN p_origen_lat DECIMAL(10,7), IN p_origen_lng DECIMAL(10,7),
    IN p_destino_direccion VARCHAR(255), IN p_destino_lat DECIMAL(10,7), IN p_destino_lng DECIMAL(10,7),
    IN p_monto_total DECIMAL(8,2), OUT p_id BIGINT
)
BEGIN
    INSERT INTO viaje (rider_id, conductor_id, tarifa_id,
                        origen_direccion, origen_lat, origen_lng,
                        destino_direccion, destino_lat, destino_lng,
                        estado, monto_total, fecha_solicitud)
    VALUES (p_rider_id, NULL, p_tarifa_id,
            p_origen_direccion, p_origen_lat, p_origen_lng,
            p_destino_direccion, p_destino_lat, p_destino_lng,
            'SOLICITADO', p_monto_total, NOW());
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_viaje_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, rider_id, conductor_id, tarifa_id, origen_direccion, origen_lat, origen_lng,
           destino_direccion, destino_lat, destino_lng, estado, monto_total,
           fecha_solicitud, fecha_inicio, fecha_fin
    FROM viaje WHERE id = p_id;
END //

CREATE PROCEDURE sp_viaje_listar_por_rider(IN p_rider_id BIGINT)
BEGIN
    SELECT id, rider_id, conductor_id, tarifa_id, origen_direccion, origen_lat, origen_lng,
           destino_direccion, destino_lat, destino_lng, estado, monto_total,
           fecha_solicitud, fecha_inicio, fecha_fin
    FROM viaje WHERE rider_id = p_rider_id ORDER BY fecha_solicitud DESC;
END //

CREATE PROCEDURE sp_viaje_listar_por_conductor(IN p_conductor_id BIGINT)
BEGIN
    SELECT id, rider_id, conductor_id, tarifa_id, origen_direccion, origen_lat, origen_lng,
           destino_direccion, destino_lat, destino_lng, estado, monto_total,
           fecha_solicitud, fecha_inicio, fecha_fin
    FROM viaje WHERE conductor_id = p_conductor_id ORDER BY fecha_solicitud DESC;
END //

CREATE PROCEDURE sp_viaje_listar_solicitados()
BEGIN
    SELECT id, rider_id, conductor_id, tarifa_id, origen_direccion, origen_lat, origen_lng,
           destino_direccion, destino_lat, destino_lng, estado, monto_total,
           fecha_solicitud, fecha_inicio, fecha_fin
    FROM viaje WHERE estado = 'SOLICITADO' ORDER BY fecha_solicitud ASC;
END //

CREATE PROCEDURE sp_viaje_aceptar(IN p_viaje_id BIGINT, IN p_conductor_id BIGINT)
BEGIN
    UPDATE viaje SET conductor_id = p_conductor_id, estado = 'ACEPTADO'
    WHERE id = p_viaje_id AND estado = 'SOLICITADO';
    UPDATE conductor SET estado = 'OCUPADO' WHERE id = p_conductor_id;
END //

CREATE PROCEDURE sp_viaje_iniciar(IN p_viaje_id BIGINT)
BEGIN
    UPDATE viaje SET estado = 'EN_CURSO', fecha_inicio = NOW()
    WHERE id = p_viaje_id AND estado = 'ACEPTADO';
END //

CREATE PROCEDURE sp_viaje_finalizar(IN p_viaje_id BIGINT)
BEGIN
    DECLARE v_conductor_id BIGINT;
    SELECT conductor_id INTO v_conductor_id FROM viaje WHERE id = p_viaje_id;
    UPDATE viaje SET estado = 'FINALIZADO', fecha_fin = NOW()
    WHERE id = p_viaje_id AND estado = 'EN_CURSO';
    UPDATE conductor SET estado = 'DISPONIBLE' WHERE id = v_conductor_id;
END //

CREATE PROCEDURE sp_viaje_cancelar(IN p_viaje_id BIGINT)
BEGIN
    DECLARE v_conductor_id BIGINT;
    SELECT conductor_id INTO v_conductor_id FROM viaje WHERE id = p_viaje_id;
    UPDATE viaje SET estado = 'CANCELADO'
    WHERE id = p_viaje_id AND estado IN ('SOLICITADO', 'ACEPTADO');
    IF v_conductor_id IS NOT NULL THEN
        UPDATE conductor SET estado = 'DISPONIBLE' WHERE id = v_conductor_id;
    END IF;
END //

CREATE PROCEDURE sp_viaje_historial_por_rider(IN p_rider_id BIGINT)
BEGIN
    SELECT v.id, v.fecha_solicitud, v.fecha_inicio, v.fecha_fin,
           v.origen_direccion, v.destino_direccion, v.estado, v.monto_total,
           v.origen_lat, v.origen_lng, v.destino_lat, v.destino_lng,
           u.nombre AS c_nombre, u.apellido AS c_apellido,
           veh.color AS v_color, veh.marca AS v_marca, veh.modelo AS v_modelo, veh.placa AS v_placa,
           c.calificacion_promedio AS c_calificacion
    FROM viaje v
    LEFT JOIN conductor c ON v.conductor_id = c.id
    LEFT JOIN usuario u ON c.usuario_id = u.id
    LEFT JOIN vehiculo veh ON c.vehiculo_id = veh.id
    WHERE v.rider_id = p_rider_id
    ORDER BY v.fecha_solicitud DESC;
END //

CREATE PROCEDURE sp_ubicacion_insertar(
    IN p_viaje_id BIGINT, IN p_lat DECIMAL(10,7), IN p_lng DECIMAL(10,7), OUT p_id BIGINT
)
BEGIN
    INSERT INTO ubicacion (viaje_id, lat, lng, timestamp) VALUES (p_viaje_id, p_lat, p_lng, NOW());
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_ubicacion_listar_por_viaje(IN p_viaje_id BIGINT)
BEGIN
    SELECT id, viaje_id, lat, lng, timestamp FROM ubicacion WHERE viaje_id = p_viaje_id ORDER BY timestamp ASC;
END //

CREATE PROCEDURE sp_ubicacion_obtener_ultima(IN p_viaje_id BIGINT)
BEGIN
    SELECT id, viaje_id, lat, lng, timestamp FROM ubicacion
    WHERE viaje_id = p_viaje_id ORDER BY timestamp DESC, id DESC LIMIT 1;
END //

CREATE PROCEDURE sp_pago_insertar(
    IN p_viaje_id BIGINT, IN p_metodo VARCHAR(20), IN p_monto DECIMAL(8,2), OUT p_id BIGINT
)
BEGIN
    INSERT INTO pago (viaje_id, metodo, monto, estado, fecha_pago)
    VALUES (p_viaje_id, p_metodo, p_monto, 'PENDIENTE', NOW());
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_pago_buscar_por_id(IN p_id BIGINT)
BEGIN
    SELECT id, viaje_id, metodo, monto, estado, fecha_pago FROM pago WHERE id = p_id;
END //

CREATE PROCEDURE sp_pago_buscar_por_viaje(IN p_viaje_id BIGINT)
BEGIN
    SELECT id, viaje_id, metodo, monto, estado, fecha_pago FROM pago WHERE viaje_id = p_viaje_id;
END //

CREATE PROCEDURE sp_pago_actualizar_estado(IN p_id BIGINT, IN p_estado VARCHAR(20))
BEGIN
    UPDATE pago SET estado = p_estado WHERE id = p_id;
END //

CREATE PROCEDURE sp_calificacion_insertar(
    IN p_viaje_id BIGINT, IN p_usuario_id BIGINT, IN p_puntaje TINYINT, IN p_comentario VARCHAR(500), OUT p_id BIGINT
)
BEGIN
    INSERT INTO calificacion (viaje_id, usuario_id, puntaje, comentario, fecha)
    VALUES (p_viaje_id, p_usuario_id, p_puntaje, p_comentario, NOW());
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_calificacion_listar_por_viaje(IN p_viaje_id BIGINT)
BEGIN
    SELECT id, viaje_id, usuario_id, puntaje, comentario, fecha FROM calificacion WHERE viaje_id = p_viaje_id;
END //

CREATE PROCEDURE sp_calificacion_listar_recibidas_por_conductor(IN p_conductor_usuario_id BIGINT)
BEGIN
    SELECT c.id, c.viaje_id, c.usuario_id, c.puntaje, c.comentario, c.fecha
    FROM calificacion c
    INNER JOIN viaje v ON v.id = c.viaje_id
    INNER JOIN conductor cond ON cond.id = v.conductor_id
    WHERE cond.usuario_id = p_conductor_usuario_id AND c.usuario_id = v.rider_id;
END //

CREATE PROCEDURE sp_metodo_pago_insertar(
    IN p_usuario_id BIGINT, IN p_tipo VARCHAR(20), IN p_detalle VARCHAR(100),
    IN p_predeterminado BOOLEAN, OUT p_id BIGINT
)
BEGIN
    IF p_predeterminado = TRUE THEN
        UPDATE metodo_pago SET predeterminado = FALSE WHERE usuario_id = p_usuario_id;
    END IF;
    INSERT INTO metodo_pago (usuario_id, tipo, detalle, predeterminado)
    VALUES (p_usuario_id, p_tipo, p_detalle, p_predeterminado);
    SET p_id = LAST_INSERT_ID();
END //

CREATE PROCEDURE sp_metodo_pago_listar_por_usuario(IN p_usuario_id BIGINT)
BEGIN
    SELECT id, usuario_id, tipo, detalle, predeterminado FROM metodo_pago WHERE usuario_id = p_usuario_id;
END //

CREATE PROCEDURE sp_metodo_pago_eliminar(IN p_id BIGINT)
BEGIN
    DELETE FROM metodo_pago WHERE id = p_id;
END //

DELIMITER ;
