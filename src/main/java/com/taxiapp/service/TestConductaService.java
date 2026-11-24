package com.taxiapp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestConductaService {

	public static final int PREGUNTAS_POR_INTENTO = 10;
	public static final int NOTA_MINIMA = 8;
	public static final int MAX_INTENTOS = 2;
	public static final int DIAS_BLOQUEO = 15;

	private final JdbcTemplate jdbc;

	public TestConductaService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Map<String, Object> estado(String email, String telefono, String rol) {
		Map<String, Object> out = new LinkedHashMap<>();
		LocalDateTime hasta = bloqueoHasta(email, telefono);
		out.put("bloqueado", hasta != null && hasta.isAfter(LocalDateTime.now()));
		out.put("bloqueadoHasta", hasta);
		out.put("intentosUsados", contarIntentos(email, telefono, rol));
		out.put("intentosMaximos", MAX_INTENTOS);
		out.put("notaMinima", NOTA_MINIMA);
		out.put("preguntasPorIntento", PREGUNTAS_POR_INTENTO);
		out.put("aprobado", yaAprobado(email, telefono, rol));
		return out;
	}

	public List<Map<String, Object>> preguntasAleatorias(String rol) {
		String r = rolOk(rol);
		List<Map<String, Object>> todas = jdbc.queryForList(
				"SELECT id, rol, enunciado, opcion_a AS opcionA, opcion_b AS opcionB, opcion_c AS opcionC, opcion_d AS opcionD FROM test_pregunta WHERE rol = ?",
				r);
		Collections.shuffle(todas);
		if (todas.size() <= PREGUNTAS_POR_INTENTO) {
			return todas;
		}
		return new ArrayList<>(todas.subList(0, PREGUNTAS_POR_INTENTO));
	}

	public Map<String, Object> calificar(String email, String telefono, String rol,
			List<Map<String, Object>> respuestas) {
		String r = rolOk(rol);
		String mail = email == null ? "" : email.trim().toLowerCase();
		String tel = telefono == null ? "" : telefono.trim();
		if (mail.isBlank()) {
			throw new IllegalArgumentException("Falta email");
		}

		LocalDateTime hasta = bloqueoHasta(mail, tel);
		if (hasta != null && hasta.isAfter(LocalDateTime.now())) {
			return Map.of(
					"aprobado", false,
					"bloqueado", true,
					"bloqueadoHasta", hasta.toString(),
					"mensaje", "Por seguridad no puedes repetir el test hasta el " + hasta.toLocalDate() + ".");
		}

		int usados = contarIntentos(mail, tel, r);
		if (usados >= MAX_INTENTOS) {
			bloquear(mail, tel);
			return Map.of(
					"aprobado", false,
					"bloqueado", true,
					"mensaje", "Ya usaste los 2 intentos. Debes esperar 15 días.");
		}

		if (respuestas == null || respuestas.size() != PREGUNTAS_POR_INTENTO) {
			throw new IllegalArgumentException("Debes responder exactamente " + PREGUNTAS_POR_INTENTO + " preguntas");
		}

		int nota = 0;
		Set<Long> vistos = new HashSet<>();
		for (Map<String, Object> resp : respuestas) {
			Long id = asLong(resp.get("preguntaId"));
			String opcion = texto(resp.get("opcion")).toUpperCase();
			if (id == null || vistos.contains(id)) {
				continue;
			}
			vistos.add(id);
			List<Map<String, Object>> filas = jdbc.queryForList(
					"SELECT correcta FROM test_pregunta WHERE id = ? AND rol = ?", id, r);
			if (!filas.isEmpty() && opcion.equalsIgnoreCase(texto(filas.get(0).get("correcta")))) {
				nota++;
			}
		}
		double porcentaje = Math.round((nota * 1000.0) / PREGUNTAS_POR_INTENTO) / 10.0;
		boolean aprobado = nota >= NOTA_MINIMA;
		int nro = usados + 1;
		jdbc.update(
				"INSERT INTO test_intento (email, telefono, rol, nro_intento, nota, porcentaje, aprobado) VALUES (?,?,?,?,?,?,?)",
				mail, tel, r, nro, nota, porcentaje, aprobado);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("aprobado", aprobado);
		out.put("nota", nota);
		out.put("notaMinima", NOTA_MINIMA);
		out.put("porcentaje", porcentaje);
		out.put("nroIntento", nro);
		out.put("intentosRestantes", MAX_INTENTOS - nro);
		out.put("bloqueado", false);

		if (!aprobado && nro >= MAX_INTENTOS) {
			bloquear(mail, tel);
			out.put("bloqueado", true);
			out.put("bloqueadoHasta", LocalDateTime.now().plusDays(DIAS_BLOQUEO).toString());
			out.put("mensaje", "Desaprobaste el segundo intento. Por seguridad de pasajeros y conductores "
					+ "debes esperar 15 días para volver a intentarlo.");
		} else if (!aprobado) {
			out.put("mensaje", "Desaprobaste (nota " + nota + "/" + PREGUNTAS_POR_INTENTO + "). Te queda 1 intento.");
		} else {
			out.put("mensaje", "Aprobaste el test de conducta (" + nota + "/" + PREGUNTAS_POR_INTENTO + "). Puedes seguir el registro.");
		}
		return out;
	}

	public void olvidarIntentos(String email, String telefono) {
		String mail = email == null ? "" : email.trim().toLowerCase();
		String tel = telefono == null ? "" : telefono.trim();
		if (!mail.isBlank()) {
			jdbc.update("DELETE FROM test_intento WHERE email = ?", mail);
			jdbc.update("DELETE FROM test_bloqueo WHERE email = ?", mail);
		}
		if (!tel.isBlank()) {
			jdbc.update("DELETE FROM test_intento WHERE telefono = ?", tel);
			jdbc.update("DELETE FROM test_bloqueo WHERE telefono = ?", tel);
		}
	}

	private void bloquear(String email, String telefono) {
		LocalDateTime hasta = LocalDateTime.now().plusDays(DIAS_BLOQUEO);
		jdbc.update(
				"INSERT INTO test_bloqueo (email, telefono, bloqueado_hasta, motivo) VALUES (?,?,?,?) "
						+ "ON DUPLICATE KEY UPDATE bloqueado_hasta = VALUES(bloqueado_hasta), telefono = VALUES(telefono)",
				email, telefono, hasta, "Dos intentos desaprobados");
	}

	private LocalDateTime bloqueoHasta(String email, String telefono) {
		String mail = email == null ? "" : email.trim().toLowerCase();
		String tel = telefono == null ? "" : telefono.trim();
		List<Map<String, Object>> filas = jdbc.queryForList(
				"SELECT bloqueado_hasta FROM test_bloqueo WHERE email = ? OR (telefono IS NOT NULL AND telefono <> '' AND telefono = ?) ORDER BY bloqueado_hasta DESC LIMIT 1",
				mail, tel);
		if (filas.isEmpty()) {
			return null;
		}
		Object v = filas.get(0).get("bloqueado_hasta");
		if (v instanceof LocalDateTime ldt) {
			return ldt;
		}
		if (v instanceof java.sql.Timestamp ts) {
			return ts.toLocalDateTime();
		}
		return v == null ? null : LocalDateTime.parse(v.toString().replace(" ", "T"));
	}

	private boolean yaAprobado(String email, String telefono, String rol) {
		try {
			Integer n = jdbc.queryForObject(
					"SELECT COUNT(*) FROM test_intento WHERE aprobado = 1 AND rol = ? AND (email = ? OR (telefono IS NOT NULL AND telefono <> '' AND telefono = ?))",
					Integer.class, rolOk(rol), email == null ? "" : email.trim().toLowerCase(),
					telefono == null ? "" : telefono);
			return n != null && n > 0;
		} catch (Exception e) {
			return false;
		}
	}

	private int contarIntentos(String email, String telefono, String rol) {
		String mail = email == null ? "" : email.trim().toLowerCase();
		String tel = telefono == null ? "" : telefono.trim();
		LocalDateTime hasta = bloqueoHasta(mail, tel);
		boolean vencido = hasta != null && !hasta.isAfter(LocalDateTime.now());
		Integer n = vencido
				? jdbc.queryForObject(
						"SELECT COUNT(*) FROM test_intento WHERE rol = ? AND fecha > ? AND (email = ? OR (telefono IS NOT NULL AND telefono <> '' AND telefono = ?))",
						Integer.class, rol, hasta, mail, tel)
				: jdbc.queryForObject(
						"SELECT COUNT(*) FROM test_intento WHERE rol = ? AND (email = ? OR (telefono IS NOT NULL AND telefono <> '' AND telefono = ?))",
						Integer.class, rol, mail, tel);
		return n == null ? 0 : n;
	}

	private static String rolOk(String rol) {
		String r = rol == null ? "" : rol.trim().toUpperCase();
		if (!r.equals("RIDER") && !r.equals("CONDUCTOR")) {
			throw new IllegalArgumentException("rol debe ser RIDER o CONDUCTOR");
		}
		return r;
	}

	private static String texto(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static Long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(texto(v));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
