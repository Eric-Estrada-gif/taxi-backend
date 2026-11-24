package com.taxiapp.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RegistroPadronService {

	private final JdbcTemplate jdbc;

	public RegistroPadronService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Map<String, Object> dni(String dni) {
		return unaFila("SELECT dni, nombres, fecha_caducidad AS fechaCaducidad, estado FROM padron_dni WHERE dni = ?",
				dni);
	}

	public Map<String, Object> licencia(String dni) {
		return unaFila(
				"SELECT dni, nombres, numero_licencia AS numeroLicencia, categoria, fecha_vencimiento AS fechaVencimiento, estado FROM padron_licencia WHERE dni = ?",
				dni);
	}

	public Map<String, Object> soat(String placa) {
		return unaFila(
				"SELECT placa, dni_titular AS dniTitular, compania, fecha_inicio AS fechaInicio, fecha_fin AS fechaFin, estado, marca, modelo, color, clase FROM padron_soat WHERE placa = ?",
				normalizarPlaca(placa));
	}

	public Map<String, Object> resumenPadron() {
		Integer dni = jdbc.queryForObject("SELECT COUNT(*) FROM padron_dni", Integer.class);
		Integer lic = jdbc.queryForObject("SELECT COUNT(*) FROM padron_licencia", Integer.class);
		Integer soat = jdbc.queryForObject("SELECT COUNT(*) FROM padron_soat", Integer.class);
		List<Map<String, Object>> ejemplos = jdbc.queryForList("""
				SELECT d.dni, d.nombres, d.estado AS dniEstado,
				       l.numero_licencia AS numeroLicencia, l.estado AS licenciaEstado,
				       s.placa, s.estado AS soatEstado
				FROM padron_dni d
				LEFT JOIN padron_licencia l ON l.dni = d.dni
				LEFT JOIN padron_soat s ON s.dni_titular = d.dni
				ORDER BY d.dni
				""");
		return Map.of(
				"conteos", Map.of("dni", n(dni), "licencia", n(lic), "soat", n(soat)),
				"ejemplos", ejemplos,
				"pruebaAprobado", Map.of("dni", "41000001", "nombres", "ANA TORRES QUISPE",
						"numeroLicencia", "L-450001", "placa", "AAA101"),
				"pruebaSoatVencido", Map.of("dni", "41000021", "nombres", "ANDRES QUISPE MAMA",
						"numeroLicencia", "L-450021", "placa", "AAA121"));
	}

	private static int n(Integer v) {
		return v == null ? 0 : v;
	}

	public Map<String, Object> verificar(Long conductorId, String dni, String nombresDeclarados,
			String numeroLicencia, String placa) {
		String dniOk = dni == null ? "" : dni.trim();
		String placaOk = normalizarPlaca(placa);
		Map<String, Object> fichaDni = dni(dniOk);
		Map<String, Object> fichaLic = licencia(dniOk);
		Map<String, Object> fichaSoat = soat(placaOk);

		List<String> problemas = new ArrayList<>();
		String estDni = texto(fichaDni == null ? null : fichaDni.get("estado"));
		String estLic = texto(fichaLic == null ? null : fichaLic.get("estado"));
		String estSoat = texto(fichaSoat == null ? null : fichaSoat.get("estado"));
		boolean dniOkEstado = "VIGENTE".equals(estDni) || "POR_RENOVAR".equals(estDni);
		boolean licOkEstado = "VIGENTE".equals(estLic) || "POR_RENOVAR".equals(estLic);
		boolean soatVigente = "VIGENTE".equals(estSoat);
		boolean soatVencido = "VENCIDO".equals(estSoat);
		boolean soatNoRegistra = fichaSoat == null || "NO_REGISTRA".equals(estSoat);
		boolean dniPorVencer = "POR_RENOVAR".equals(estDni)
				|| ("VIGENTE".equals(estDni) && porVencer(fichaDni.get("fechaCaducidad")));
		boolean licPorVencer = "POR_RENOVAR".equals(estLic)
				|| ("VIGENTE".equals(estLic) && porVencer(fichaLic.get("fechaVencimiento")));
		boolean soatPorVencer = "POR_RENOVAR".equals(estSoat)
				|| (soatVigente && porVencer(fichaSoat.get("fechaFin")));

		if (fichaDni == null) {
			problemas.add("No hay DNI en el padron");
		} else if (!dniOkEstado) {
			problemas.add("El documento de identidad esta vencido");
		} else if (dniPorVencer) {
			problemas.add("El DNI se aproxima a su vencimiento y requiere actualizar");
		}

		if (fichaLic == null) {
			problemas.add("No hay licencia en el padron");
		} else if (!licOkEstado) {
			problemas.add("La licencia de conducir esta vencida");
		} else if (licPorVencer) {
			problemas.add("La licencia se aproxima a su vencimiento y requiere actualizar");
		}

		boolean nombresOk = true;
		if (fichaDni != null && fichaLic != null) {
			nombresOk = mismoNombre(texto(fichaDni.get("nombres")), texto(fichaLic.get("nombres")))
					&& (nombresDeclarados == null || nombresDeclarados.isBlank()
							|| mismoNombre(nombresDeclarados, texto(fichaDni.get("nombres"))));
			if (!nombresOk) {
				problemas.add("El nombre del DNI no coincide con el de la licencia");
			}
		}
		if (fichaLic != null && numeroLicencia != null && !numeroLicencia.isBlank()
				&& !numeroLicencia.trim().equalsIgnoreCase(texto(fichaLic.get("numeroLicencia")))) {
			problemas.add("El numero de licencia no coincide con el padron");
			nombresOk = false;
		}

		if (soatNoRegistra) {
			problemas.add("No hay SOAT registrado para esa placa");
		} else if (soatVencido) {
			problemas.add("El SOAT esta vencido");
		} else if (soatPorVencer) {
			problemas.add("El SOAT se aproxima a su vencimiento y requiere actualizar");
		}

		boolean graves = (fichaDni == null || !dniOkEstado) || (fichaLic == null || !licOkEstado) || !nombresOk
				|| soatNoRegistra;
		boolean soloRenovacion = !graves && (dniPorVencer || licPorVencer || soatPorVencer || soatVencido);
		String veredicto;
		String mensaje;
		if (soloRenovacion) {
			veredicto = "OBSERVADO";
			mensaje = "Falta renovar un documento. El registro queda guardado para retomarlo con el mismo correo.";
		} else if (problemas.isEmpty() && soatVigente) {
			veredicto = "APROBADO";
			mensaje = "Documentos en regla. Puedes completar el registro y ponerte en linea.";
		} else {
			veredicto = "RECHAZADO";
			mensaje = "No puedes completar el registro: " + String.join(". ", problemas) + ".";
		}

		Map<String, Object> detalle = new LinkedHashMap<>();
		detalle.put("dni", fichaDni);
		detalle.put("licencia", fichaLic);
		detalle.put("soat", fichaSoat);
		detalle.put("problemas", problemas);

		jdbc.update(
				"INSERT INTO verificacion_conductor (conductor_id, dni, placa, veredicto, mensaje, detalle_json) VALUES (?,?,?,?,?,?)",
				conductorId, dniOk, placaOk, veredicto, mensaje, problemas.toString());

		if ("APROBADO".equals(veredicto) || "OBSERVADO".equals(veredicto)) {
			guardarUnidad(conductorId, fichaLic, fichaSoat);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("veredicto", veredicto);
		out.put("mensaje", mensaje);
		out.put("problemas", problemas);
		out.put("chequeos", detalle);
		return out;
	}

	private void guardarUnidad(Long conductorId, Map<String, Object> fichaLic, Map<String, Object> fichaSoat) {
		if (conductorId == null || fichaLic == null || fichaSoat == null) {
			return;
		}
		String numero = texto(fichaLic.get("numeroLicencia"));
		String placa = normalizarPlaca(texto(fichaSoat.get("placa")));
		if (numero.isBlank() || placa.isBlank()) {
			return;
		}
		java.sql.Date venceLicencia = fechaSql(fichaLic.get("fechaVencimiento"));
		java.sql.Date venceSoat = fechaSql(fichaSoat.get("fechaFin"));
		String marca = texto(fichaSoat.get("marca"));
		String modelo = texto(fichaSoat.get("modelo"));
		String color = texto(fichaSoat.get("color"));
		if (marca.isBlank()) {
			marca = "Auto";
		}
		if (modelo.isBlank()) {
			modelo = "Taxi";
		}
		jdbc.update(
				"UPDATE conductor SET numero_licencia = ?, fecha_vencimiento_licencia = ? WHERE id = ?",
				numero, venceLicencia, conductorId);

		Long vehiculoId = null;
		try {
			vehiculoId = jdbc.queryForObject(
					"SELECT id FROM vehiculo WHERE conductor_id = ? OR placa = ? ORDER BY conductor_id = ? DESC LIMIT 1",
					Long.class, conductorId, placa, conductorId);
		} catch (EmptyResultDataAccessException ignored) {
			vehiculoId = null;
		}
		if (vehiculoId == null) {
			jdbc.update(
					"""
							INSERT INTO vehiculo (placa, marca, modelo, anio, color, soat_vigente_hasta, conductor_id)
							VALUES (?, ?, ?, ?, ?, ?, ?)
							""",
					placa, marca, modelo, null, color.isBlank() ? null : color, venceSoat, conductorId);
			try {
				vehiculoId = jdbc.queryForObject("SELECT id FROM vehiculo WHERE placa = ? LIMIT 1", Long.class, placa);
			} catch (EmptyResultDataAccessException ignored) {
				vehiculoId = null;
			}
		} else {
			jdbc.update(
					"""
							UPDATE vehiculo SET placa = ?, marca = ?, modelo = ?, color = ?, soat_vigente_hasta = ?, conductor_id = ?
							WHERE id = ?
							""",
					placa, marca, modelo, color.isBlank() ? null : color, venceSoat, conductorId, vehiculoId);
		}
		if (vehiculoId != null) {
			jdbc.update("UPDATE conductor SET vehiculo_id = ? WHERE id = ?", vehiculoId, conductorId);
		}
	}

	private static java.sql.Date fechaSql(Object fecha) {
		if (fecha == null) {
			return null;
		}
		java.time.LocalDate dia;
		if (fecha instanceof java.sql.Date sql) {
			dia = sql.toLocalDate();
		} else if (fecha instanceof java.time.LocalDate ld) {
			dia = ld;
		} else {
			String t = fecha.toString();
			if (t.length() < 10) {
				return null;
			}
			dia = java.time.LocalDate.parse(t.substring(0, 10));
		}
		return java.sql.Date.valueOf(dia);
	}

	private Map<String, Object> unaFila(String sql, String clave) {
		if (clave == null || clave.isBlank()) {
			return null;
		}
		try {
			return jdbc.queryForMap(sql, clave.trim());
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	private static boolean porVencer(Object fecha) {
		if (fecha == null) {
			return false;
		}
		java.time.LocalDate dia;
		if (fecha instanceof java.sql.Date sql) {
			dia = sql.toLocalDate();
		} else if (fecha instanceof java.time.LocalDate ld) {
			dia = ld;
		} else {
			String t = fecha.toString();
			if (t.length() < 10) {
				return false;
			}
			dia = java.time.LocalDate.parse(t.substring(0, 10));
		}
		long dias = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), dia);
		return dias >= 0 && dias <= 15;
	}

	private static String texto(Object v) {
		return v == null ? "" : v.toString();
	}

	private static String normalizarPlaca(String placa) {
		return placa == null ? "" : placa.trim().toUpperCase().replace(" ", "").replace("-", "");
	}

	static boolean mismoNombre(String a, String b) {
		return norma(a).equals(norma(b));
	}

	private static String norma(String s) {
		if (s == null) {
			return "";
		}
		String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		return n.toUpperCase().replaceAll("[^A-Z0-9]", "");
	}
}
