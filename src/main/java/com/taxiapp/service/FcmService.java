package com.taxiapp.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FcmService {

	private static final Logger log = LoggerFactory.getLogger(FcmService.class);

	private final JdbcTemplate jdbcTemplate;
	private final RestClient restClient = RestClient.create();

	@Value("${fcm.enabled:false}")
	private boolean enabled;

	@Value("${fcm.server-key:}")
	private String serverKey;

	public FcmService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void guardarToken(Long usuarioId, String token) {
		if (token == null || token.isBlank()) {
			return;
		}
		try {
			jdbcTemplate.update(
					"""
							INSERT INTO dispositivo_push (usuario_id, token, actualizado)
							VALUES (?, ?, NOW())
							ON DUPLICATE KEY UPDATE token = VALUES(token), actualizado = NOW()
							""",
					usuarioId, token.trim());
		} catch (DataAccessException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Ejecuta sql/dispositivo_push.sql en MySQL antes de registrar el token FCM");
		}
	}

	public void notificarConductoresDisponibles(Long viajeId, String origen, String destino, BigDecimal monto) {
		if (!enabled || serverKey == null || serverKey.isBlank()) {
			return;
		}

		List<String> tokens;
		try {
			tokens = jdbcTemplate.queryForList(
					"""
							SELECT d.token
							FROM dispositivo_push d
							INNER JOIN conductor c ON c.usuario_id = d.usuario_id
							WHERE c.estado = 'DISPONIBLE'
							""",
					String.class);
		} catch (Exception ex) {
			log.warn("No se pudieron leer tokens FCM. Ejecuta sql/dispositivo_push.sql. {}", ex.getMessage());
			return;
		}

		if (tokens.isEmpty()) {
			return;
		}

		String titulo = "Nuevo viaje";
		String cuerpo = (origen != null ? origen : "Origen") + " → " + (destino != null ? destino : "Destino");
		if (monto != null) {
			cuerpo = cuerpo + " · S/ " + monto;
		}

		for (String token : tokens) {
			enviar(token, titulo, cuerpo, viajeId);
		}
	}

	private void enviar(String token, String titulo, String cuerpo, Long viajeId) {
		try {
			Map<String, Object> payload = Map.of(
					"to", token,
					"priority", "high",
					"notification", Map.of("title", titulo, "body", cuerpo),
					"data", Map.of(
							"tipo", "VIAJE_SOLICITADO",
							"viajeId", String.valueOf(viajeId)));

			restClient.post()
					.uri("https://fcm.googleapis.com/fcm/send")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Authorization", "key=" + serverKey)
					.body(payload)
					.retrieve()
					.toBodilessEntity();
		} catch (Exception ex) {
			log.warn("Fallo enviando FCM: {}", ex.getMessage());
		}
	}
}
