package com.taxiapp.admin;

import java.util.List;

public record AdminTabla(
		String clave,
		String titulo,
		String sqlNombre,
		String pk,
		boolean pkAuto,
		List<AdminColumna> columnas) {

	public static List<AdminTabla> todas() {
		return List.of(
				new AdminTabla("usuario", "Usuarios", "usuario", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("nombre", "Nombre", "text", false, true, true),
						col("apellido", "Apellido", "text", false, true, true),
						col("email", "Correo", "text", false, true, true),
						col("password", "Contraseña", "password", false, true, false),
						col("google_id", "Google ID", "text", false, true, false),
						col("telefono", "Teléfono", "text", false, true, true),
						col("rol", "Rol", "select:RIDER,CONDUCTOR,ADMIN", false, true, true),
						col("foto_perfil", "Foto", "text", false, true, false),
						col("sexo", "Sexo", "text", false, true, false),
						col("fecha_nacimiento", "Fecha nac.", "text", false, true, false),
						col("fecha_registro", "Registro", "datetime", false, false, true),
						col("activo", "Activo", "bool", false, true, true))),
				new AdminTabla("conductor", "Conductores", "conductor", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("usuario_id", "ID usuario", "number", false, true, true),
						col("numero_licencia", "Licencia", "text", false, true, true),
						col("fecha_vencimiento_licencia", "Vence licencia", "date", false, true, true),
						col("estado", "Estado", "select:DISPONIBLE,OCUPADO,OFFLINE", false, true, true),
						col("calificacion_promedio", "Calificación", "decimal", false, true, true),
						col("vehiculo_id", "ID vehículo", "number", false, true, true))),
				new AdminTabla("vehiculo", "Vehículos", "vehiculo", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("conductor_id", "ID conductor", "number", false, true, true),
						col("placa", "Placa", "text", false, true, true),
						col("marca", "Marca", "text", false, true, true),
						col("modelo", "Modelo", "text", false, true, true),
						col("anio", "Año", "number", false, true, true),
						col("color", "Color", "text", false, true, true),
						col("soat_vigente_hasta", "SOAT hasta", "date", false, true, true))),
				new AdminTabla("tarifa", "Tarifas", "tarifa", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("nombre", "Nombre", "text", false, true, true),
						col("tarifa_base", "Tarifa base", "decimal", false, true, true),
						col("precio_km", "Precio km", "decimal", false, true, true),
						col("precio_minuto", "Precio minuto", "decimal", false, true, true),
						col("vigente_desde", "Vigente desde", "date", false, true, true),
						col("activo", "Activo", "bool", false, true, true))),
				new AdminTabla("viaje", "Viajes", "viaje", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("rider_id", "ID pasajero", "number", false, true, true),
						col("conductor_id", "ID conductor", "number", false, true, true),
						col("tarifa_id", "ID tarifa", "number", false, true, true),
						col("origen_direccion", "Origen", "text", false, true, true),
						col("origen_lat", "Origen lat", "decimal", false, true, false),
						col("origen_lng", "Origen lng", "decimal", false, true, false),
						col("destino_direccion", "Destino", "text", false, true, true),
						col("destino_lat", "Destino lat", "decimal", false, true, false),
						col("destino_lng", "Destino lng", "decimal", false, true, false),
						col("estado", "Estado", "select:SOLICITADO,ACEPTADO,EN_CURSO,FINALIZADO,CANCELADO", false, true, true),
						col("monto_total", "Monto", "decimal", false, true, true),
						col("fecha_solicitud", "Solicitud", "datetime", false, false, true),
						col("fecha_inicio", "Inicio", "datetime", false, true, false),
						col("fecha_fin", "Fin", "datetime", false, true, false))),
				new AdminTabla("ubicacion", "Ubicaciones", "ubicacion", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("viaje_id", "ID viaje", "number", false, true, true),
						col("lat", "Lat", "decimal", false, true, true),
						col("lng", "Lng", "decimal", false, true, true),
						col("timestamp", "Fecha", "datetime", false, false, true))),
				new AdminTabla("pago", "Pagos", "pago", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("viaje_id", "ID viaje", "number", false, true, true),
						col("metodo", "Método", "select:EFECTIVO,TARJETA,BILLETERA_DIGITAL,VIP", false, true, true),
						col("monto", "Monto", "decimal", false, true, true),
						col("estado", "Estado", "select:PENDIENTE,COMPLETADO,FALLIDO,REEMBOLSADO", false, true, true),
						col("fecha_pago", "Fecha", "datetime", false, false, true))),
				new AdminTabla("metodo_pago", "Métodos de pago", "metodo_pago", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("usuario_id", "ID usuario", "number", false, true, true),
						col("tipo", "Tipo", "select:TARJETA,BILLETERA_DIGITAL", false, true, true),
						col("detalle", "Detalle", "textarea", false, true, true),
						col("predeterminado", "Predeterminado", "bool", false, true, true))),
				new AdminTabla("calificacion", "Calificaciones", "calificacion", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("viaje_id", "ID viaje", "number", false, true, true),
						col("usuario_id", "ID usuario", "number", false, true, true),
						col("puntaje", "Puntaje", "number", false, true, true),
						col("comentario", "Comentario", "textarea", false, true, true),
						col("fecha", "Fecha", "datetime", false, false, true))),
				new AdminTabla("mensajes_viaje", "Mensajes de chat", "mensajes_viaje", "id", true, List.of(
						col("id", "ID", "id", true, false, true),
						col("viaje_id", "ID viaje", "number", false, true, true),
						col("remitente_tipo", "Remitente", "select:RIDER,CONDUCTOR", false, true, true),
						col("mensaje", "Mensaje", "textarea", false, true, true),
						col("fecha_envio", "Enviado", "datetime", false, false, true))),
				new AdminTabla("dispositivo_push", "Dispositivos push", "dispositivo_push", "usuario_id", false, List.of(
						col("usuario_id", "ID usuario", "number", true, true, true),
						col("token", "Token", "textarea", false, true, true),
						col("actualizado", "Actualizado", "datetime", false, true, true))));
	}

	public static AdminTabla porClave(String clave) {
		return todas().stream()
				.filter(t -> t.clave().equals(clave))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Tabla no permitida"));
	}

	private static AdminColumna col(String nombre, String etiqueta, String tipo, boolean pk, boolean editable,
			boolean enLista) {
		return new AdminColumna(nombre, etiqueta, tipo, pk, editable, enLista);
	}
}
