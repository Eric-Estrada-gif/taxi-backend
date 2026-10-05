package com.taxiapp.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxiapp.exception.MapsApiException;

@Service
public class GoogleMapsService {

	@Value("${google.maps.api.key}")
	private String apiKey;

	private final RestClient restClient = RestClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public record DistanciaResultado(double distanciaKm, double duracionMinutos) {
	}

	public record LugarSugerenciaDTO(String description, String placeId) {
	}

	public DistanciaResultado calcularDistancia(BigDecimal origenLat, BigDecimal origenLng,
			BigDecimal destinoLat, BigDecimal destinoLng) {
		try {
			String url = String.format(
					"https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s,%s&destinations=%s,%s&mode=driving&departure_time=now&key=%s",
					origenLat, origenLng, destinoLat, destinoLng, apiKey);

			String response = restClient.get()
					.uri(url)
					.retrieve()
					.body(String.class);

			JsonNode root = objectMapper.readTree(response);
			String topStatus = root.path("status").asText();
			if (!"OK".equals(topStatus)) {
				throw new MapsApiException("Google Distance Matrix: " + topStatus);
			}

			if (root.path("rows").isArray() && root.path("rows").size() > 0) {
				JsonNode element = root.path("rows").get(0).path("elements").get(0);
				if ("OK".equals(element.path("status").asText())) {
					double distanciaMetros = element.path("distance").path("value").asDouble();
					double distanciaKm = distanciaMetros / 1000.0;

					JsonNode durationNode = element.has("duration_in_traffic")
							? element.path("duration_in_traffic")
							: element.path("duration");

					double duracionSegundos = durationNode.path("value").asDouble();
					double duracionMinutos = duracionSegundos / 60.0;

					return new DistanciaResultado(distanciaKm, duracionMinutos);
				}
				throw new MapsApiException("Google Distance Matrix element: " + element.path("status").asText());
			}
			throw new MapsApiException("Google Distance Matrix no devolvio filas");
		} catch (MapsApiException e) {
			throw e;
		} catch (Exception e) {
			throw new MapsApiException("Error al consultar Google Maps", e);
		}
	}

	public List<LugarSugerenciaDTO> obtenerSugerencias(String input) {
		List<LugarSugerenciaDTO> lista = new ArrayList<>();
		try {
			String inputEncoded = URLEncoder.encode(input, StandardCharsets.UTF_8);
			String url = String.format(
					"https://maps.googleapis.com/maps/api/place/autocomplete/json?input=%s&language=es&components=country:pe&key=%s",
					inputEncoded, apiKey);

			String response = restClient.get()
					.uri(url)
					.retrieve()
					.body(String.class);

			JsonNode root = objectMapper.readTree(response);
			String status = root.path("status").asText();
			if ("ZERO_RESULTS".equals(status)) {
				return lista;
			}
			if (!"OK".equals(status)) {
				throw new MapsApiException("Google Places: " + status);
			}

			JsonNode predictions = root.path("predictions");
			if (predictions.isArray()) {
				for (JsonNode pred : predictions) {
					lista.add(new LugarSugerenciaDTO(
							pred.path("description").asText(),
							pred.path("place_id").asText()));
				}
			}
			return lista;
		} catch (MapsApiException e) {
			throw e;
		} catch (Exception e) {
			throw new MapsApiException("Error al obtener sugerencias de Google Places", e);
		}
	}
}
