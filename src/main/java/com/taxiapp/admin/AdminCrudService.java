package com.taxiapp.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminCrudService {

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;

	public AdminCrudService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	public List<AdminTabla> tablas() {
		return AdminTabla.todas();
	}

	public long contar(AdminTabla tabla) {
		Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabla.sqlNombre(), Long.class);
		return n == null ? 0 : n;
	}

	public List<Map<String, Object>> listar(AdminTabla tabla) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM " + tabla.sqlNombre() + " ORDER BY " + tabla.pk() + " DESC LIMIT 300");
	}

	public Map<String, Object> buscar(AdminTabla tabla, String id) {
		List<Map<String, Object>> filas = jdbcTemplate.queryForList(
				"SELECT * FROM " + tabla.sqlNombre() + " WHERE " + tabla.pk() + " = ?",
				convertirPk(id));
		return filas.isEmpty() ? null : filas.get(0);
	}

	public Map<String, Object> filaVacia(AdminTabla tabla) {
		Map<String, Object> fila = new LinkedHashMap<>();
		for (AdminColumna col : tabla.columnas()) {
			fila.put(col.nombre(), null);
		}
		return fila;
	}

	public List<String> opciones(AdminColumna col) {
		if (col.tipo().startsWith("select:")) {
			return List.of(col.tipo().substring("select:".length()).split(","));
		}
		return List.of();
	}

	public void guardar(AdminTabla tabla, Map<String, String> form, String idActual) {
		boolean esNuevo = idActual == null || idActual.isBlank();
		List<String> columnas = new ArrayList<>();
		List<Object> valores = new ArrayList<>();

		for (AdminColumna col : tabla.columnas()) {
			if (col.pk() && tabla.pkAuto()) {
				continue;
			}
			if (!col.editable() && !(col.pk() && !tabla.pkAuto())) {
				continue;
			}
			if ("password".equals(col.tipo())) {
				String crudo = form.get(col.nombre());
				if (crudo == null || crudo.isBlank()) {
					if (esNuevo) {
						columnas.add(col.nombre());
						valores.add(passwordEncoder.encode("cambiame"));
					}
					continue;
				}
				columnas.add(col.nombre());
				valores.add(passwordEncoder.encode(crudo));
				continue;
			}
			columnas.add(col.nombre());
			valores.add(valorDe(col, form.get(col.nombre())));
		}

		if (esNuevo) {
			String cols = String.join(", ", columnas);
			String qs = String.join(", ", columnas.stream().map(s -> "?").toList());
			jdbcTemplate.update("INSERT INTO " + tabla.sqlNombre() + " (" + cols + ") VALUES (" + qs + ")",
					valores.toArray());
			return;
		}
		String sqlSets = String.join(", ", columnas.stream().map(s -> s + " = ?").toList());
		valores.add(convertirPk(idActual));
		jdbcTemplate.update("UPDATE " + tabla.sqlNombre() + " SET " + sqlSets + " WHERE " + tabla.pk() + " = ?",
				valores.toArray());
	}

	public void eliminar(AdminTabla tabla, String id) {
		Object pk = convertirPk(id);
		jdbcTemplate.execute((Connection con) -> {
			boolean prev = con.getAutoCommit();
			con.setAutoCommit(false);
			try (var st = con.createStatement()) {
				st.execute("SET FOREIGN_KEY_CHECKS = 0");
				switch (tabla.clave()) {
					case "usuario" -> eliminarUsuario(con, (Long) pk);
					case "conductor" -> eliminarConductor(con, (Long) pk);
					case "viaje" -> eliminarViaje(con, (Long) pk);
					case "vehiculo" -> eliminarVehiculo(con, (Long) pk);
					default -> {
						try (PreparedStatement ps = con.prepareStatement(
								"DELETE FROM " + tabla.sqlNombre() + " WHERE " + tabla.pk() + " = ?")) {
							ps.setObject(1, pk);
							ps.executeUpdate();
						}
					}
				}
				st.execute("SET FOREIGN_KEY_CHECKS = 1");
				con.commit();
			} catch (Exception e) {
				con.rollback();
				try (var st = con.createStatement()) {
					st.execute("SET FOREIGN_KEY_CHECKS = 1");
				}
				if (e instanceof SQLException sql) {
					throw sql;
				}
				throw new SQLException(e);
			} finally {
				con.setAutoCommit(prev);
			}
			return null;
		});
	}

	private void eliminarUsuario(Connection con, Long usuarioId) throws Exception {
		Map<String, Object> fila = buscar(AdminTabla.porClave("usuario"), String.valueOf(usuarioId));
		String email = texto(fila, "email");
		String telefono = texto(fila, "telefono");

		ejecutarSiExiste(con, """
				DELETE mv FROM mensajes_viaje mv
				INNER JOIN viaje v ON v.id = mv.viaje_id
				WHERE v.rider_id = ? OR v.conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				""", usuarioId, usuarioId);
		ejecutarSiExiste(con, """
				DELETE p FROM pago p
				INNER JOIN viaje v ON v.id = p.viaje_id
				WHERE v.rider_id = ? OR v.conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				""", usuarioId, usuarioId);
		ejecutarSiExiste(con, """
				DELETE u FROM ubicacion u
				INNER JOIN viaje v ON v.id = u.viaje_id
				WHERE v.rider_id = ? OR v.conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				""", usuarioId, usuarioId);
		ejecutarSiExiste(con, """
				DELETE c FROM calificacion c
				LEFT JOIN viaje v ON v.id = c.viaje_id
				WHERE c.usuario_id = ? OR v.rider_id = ? OR v.conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				""", usuarioId, usuarioId, usuarioId);
		ejecutarSiExiste(con, """
				DELETE FROM viaje WHERE rider_id = ?
				OR conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				""", usuarioId, usuarioId);
		ejecutarSiExiste(con, "DELETE FROM verificacion_conductor WHERE conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)", usuarioId);
		List<Long> vehiculos = ids(con, """
				SELECT id FROM vehiculo WHERE conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)
				UNION
				SELECT vehiculo_id FROM conductor WHERE usuario_id = ? AND vehiculo_id IS NOT NULL
				""", usuarioId, usuarioId);
		ejecutarSiExiste(con, "UPDATE vehiculo SET conductor_id = NULL WHERE conductor_id IN (SELECT id FROM conductor WHERE usuario_id = ?)", usuarioId);
		ejecutarSiExiste(con, "UPDATE conductor SET vehiculo_id = NULL WHERE usuario_id = ?", usuarioId);
		ejecutarSiExiste(con, "DELETE FROM conductor WHERE usuario_id = ?", usuarioId);
		for (Long vehiculoId : vehiculos) {
			ejecutarSiExiste(con, "DELETE FROM vehiculo WHERE id = ?", vehiculoId);
		}
		ejecutarSiExiste(con, "DELETE FROM metodo_pago WHERE usuario_id = ?", usuarioId);
		ejecutarSiExiste(con, "DELETE FROM dispositivo_push WHERE usuario_id = ?", usuarioId);
		if (!email.isBlank()) {
			ejecutarSiExiste(con, "DELETE FROM test_intento WHERE email = ?", email);
			ejecutarSiExiste(con, "DELETE FROM test_bloqueo WHERE email = ?", email);
		}
		if (!telefono.isBlank()) {
			ejecutarSiExiste(con, "DELETE FROM test_intento WHERE telefono = ?", telefono);
			ejecutarSiExiste(con, "DELETE FROM test_bloqueo WHERE telefono = ?", telefono);
		}
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
			ps.setLong(1, usuarioId);
			ps.executeUpdate();
		}
	}

	private void eliminarConductor(Connection con, Long conductorId) throws Exception {
		ejecutarSiExiste(con, "DELETE mv FROM mensajes_viaje mv INNER JOIN viaje v ON v.id = mv.viaje_id WHERE v.conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "DELETE p FROM pago p INNER JOIN viaje v ON v.id = p.viaje_id WHERE v.conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "DELETE u FROM ubicacion u INNER JOIN viaje v ON v.id = u.viaje_id WHERE v.conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "DELETE c FROM calificacion c INNER JOIN viaje v ON v.id = c.viaje_id WHERE v.conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "DELETE FROM viaje WHERE conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "DELETE FROM verificacion_conductor WHERE conductor_id = ?", conductorId);
		List<Long> vehiculos = ids(con, """
				SELECT id FROM vehiculo WHERE conductor_id = ?
				UNION
				SELECT vehiculo_id FROM conductor WHERE id = ? AND vehiculo_id IS NOT NULL
				""", conductorId, conductorId);
		ejecutarSiExiste(con, "UPDATE vehiculo SET conductor_id = NULL WHERE conductor_id = ?", conductorId);
		ejecutarSiExiste(con, "UPDATE conductor SET vehiculo_id = NULL WHERE id = ?", conductorId);
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM conductor WHERE id = ?")) {
			ps.setLong(1, conductorId);
			ps.executeUpdate();
		}
		for (Long vehiculoId : vehiculos) {
			ejecutarSiExiste(con, "DELETE FROM vehiculo WHERE id = ?", vehiculoId);
		}
	}

	private void eliminarViaje(Connection con, Long viajeId) throws Exception {
		ejecutarSiExiste(con, "DELETE FROM mensajes_viaje WHERE viaje_id = ?", viajeId);
		ejecutarSiExiste(con, "DELETE FROM calificacion WHERE viaje_id = ?", viajeId);
		ejecutarSiExiste(con, "DELETE FROM pago WHERE viaje_id = ?", viajeId);
		ejecutarSiExiste(con, "DELETE FROM ubicacion WHERE viaje_id = ?", viajeId);
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM viaje WHERE id = ?")) {
			ps.setLong(1, viajeId);
			ps.executeUpdate();
		}
	}

	private void eliminarVehiculo(Connection con, Long vehiculoId) throws Exception {
		ejecutarSiExiste(con, "UPDATE conductor SET vehiculo_id = NULL WHERE vehiculo_id = ?", vehiculoId);
		try (PreparedStatement ps = con.prepareStatement("DELETE FROM vehiculo WHERE id = ?")) {
			ps.setLong(1, vehiculoId);
			ps.executeUpdate();
		}
	}

	private List<Long> ids(Connection con, String sql, Object... args) throws SQLException {
		List<Long> out = new ArrayList<>();
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					long id = rs.getLong(1);
					if (!rs.wasNull()) {
						out.add(id);
					}
				}
			}
		}
		return out;
	}

	private void ejecutarSiExiste(Connection con, String sql, Object... args) {
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			for (int i = 0; i < args.length; i++) {
				ps.setObject(i + 1, args[i]);
			}
			ps.executeUpdate();
		} catch (Exception ignored) {
			// Tabla opcional (ej. dispositivo_push) o ya vacía.
		}
	}

	private static String texto(Map<String, Object> fila, String clave) {
		if (fila == null || fila.get(clave) == null) {
			return "";
		}
		return fila.get(clave).toString().trim();
	}

	private Object valorDe(AdminColumna col, String crudo) {
		if ("bool".equals(col.tipo())) {
			return crudo != null && (crudo.equals("on") || crudo.equals("true") || crudo.equals("1"));
		}
		if (crudo == null || crudo.isBlank()) {
			return null;
		}
		try {
			return switch (col.tipo()) {
				case "number", "id" -> Long.parseLong(crudo.trim());
				case "decimal" -> new java.math.BigDecimal(crudo.trim());
				case "date" -> LocalDate.parse(crudo.trim());
				case "datetime" -> {
					String t = crudo.trim().replace(' ', 'T');
					if (t.length() == 16) {
						t = t + ":00";
					}
					yield Timestamp.valueOf(LocalDateTime.parse(t));
				}
				default -> crudo.trim();
			};
		} catch (Exception e) {
			return crudo.trim();
		}
	}

	private Object convertirPk(String id) {
		try {
			return Long.parseLong(id);
		} catch (NumberFormatException e) {
			return id;
		}
	}
}
