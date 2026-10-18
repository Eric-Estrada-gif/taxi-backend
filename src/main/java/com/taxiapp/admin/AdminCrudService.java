package com.taxiapp.admin;

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
		jdbcTemplate.update("DELETE FROM " + tabla.sqlNombre() + " WHERE " + tabla.pk() + " = ?", convertirPk(id));
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
