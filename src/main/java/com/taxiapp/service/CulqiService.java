package com.taxiapp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CulqiService {

	private final ObjectMapper json = new ObjectMapper();
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

	@Value("${culqi.secret-key:}")
	private String llavePrivada;

	public Map<String, Object> cobrarPrueba(String token, String email, String deviceId, String nombre,
			String telefono, Map<String, String> tresDs) throws Exception {
		return cobrar(token, email, deviceId, nombre, "Estrada", telefono, tresDs, 300, "RapiTrip cobro de prueba");
	}

	public Map<String, Object> cobrar(String token, String email, String deviceId, String nombre, String apellido,
			String telefono, Map<String, String> tresDs, int centimos, String descripcion) throws Exception {
		if (llavePrivada == null || llavePrivada.isBlank()) {
			throw new IllegalStateException("Falta la llave de Culqi en el servidor.");
		}
		if (centimos < 100) {
			throw new IllegalArgumentException("El monto del viaje no alcanza para cobrar con Culqi.");
		}
		var cuerpoJson = json.createObjectNode();
		cuerpoJson.put("amount", centimos);
		cuerpoJson.put("currency_code", "PEN");
		cuerpoJson.put("email", email);
		cuerpoJson.put("source_id", token);
		cuerpoJson.put("description", descripcion);
		var antifraude = cuerpoJson.putObject("antifraud_details");
		antifraude.put("first_name", nombre.isBlank() ? "Pasajero" : nombre);
		antifraude.put("last_name", apellido.isBlank() ? "RapiTrip" : apellido);
		antifraude.put("phone_number", telefono.isBlank() ? "977341754" : telefono);
		if (deviceId != null && !deviceId.isBlank()) {
			antifraude.put("device_finger_print_id", deviceId);
		}
		if (tresDs != null && tresDs.get("eci") != null && !tresDs.get("eci").isBlank()) {
			var auth = cuerpoJson.putObject("authentication_3DS");
			auth.put("eci", tresDs.getOrDefault("eci", ""));
			auth.put("xid", tresDs.getOrDefault("xid", ""));
			auth.put("cavv", tresDs.getOrDefault("cavv", ""));
			auth.put("protocolVersion", tresDs.getOrDefault("protocolVersion", ""));
			auth.put("directoryServerTransactionId", tresDs.getOrDefault("directoryServerTransactionId", ""));
		}
		String cuerpo = json.writeValueAsString(cuerpoJson);
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.culqi.com/v2/charges"))
				.timeout(Duration.ofSeconds(25))
				.header("Authorization", "Bearer " + llavePrivada)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(cuerpo))
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode nodo = json.readTree(response.body().isBlank() ? "{}" : response.body());
		Map<String, Object> out = new LinkedHashMap<>();
		String mensaje = primero(nodo, "user_message", "merchant_message");
		if (mensaje.isBlank()) {
			mensaje = primero(nodo.path("outcome"), "user_message", "merchant_message");
		}
		String codigo = primero(nodo.path("outcome"), "code", "type");
		boolean pideAuth = mensaje.toLowerCase().contains("autentic") || codigo.toUpperCase().contains("AUTENTIC");
		boolean ok = !pideAuth && response.statusCode() >= 200 && response.statusCode() < 300;
		out.put("ok", ok);
		out.put("requiereAutenticacion", pideAuth);
		if (mensaje.isBlank()) {
			String soles = String.format(java.util.Locale.US, "%.2f", centimos / 100.0);
			mensaje = ok ? "Culqi cobró S/ " + soles : "Culqi no aceptó el cobro";
		}
		out.put("mensaje", mensaje);
		if (nodo.hasNonNull("id")) {
			out.put("cargoId", nodo.get("id").asText());
		}
		return out;
	}

	private static String primero(JsonNode nodo, String... campos) {
		for (String campo : campos) {
			JsonNode valor = nodo.get(campo);
			if (valor != null && !valor.isNull() && !valor.asText("").isBlank()) {
				return valor.asText();
			}
		}
		return "";
	}
}
