package com.taxiapp.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxiapp.dto.RutaRespuestaDTO;
import com.taxiapp.exception.MapsApiException;

@Service
public class GoogleDirectionsService {

	@Value("${google.maps.api.key}")
	private String apiKey;

	private final RestClient restClient = RestClient.create();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public double[] obtenerCoordenadas(String direccion) {
		try {
			String queryFinal = direccion.toLowerCase().contains("lima") ? direccion : direccion + ", Lima, Peru";
			String encoded = java.net.URLEncoder.encode(queryFinal, java.nio.charset.StandardCharsets.UTF_8);
			String url = String.format(
					"https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
					encoded, apiKey);

			String response = restClient.get().uri(url).retrieve().body(String.class);
			JSONObject json = new JSONObject(response);
			if (!"OK".equals(json.optString("status"))) {
				throw new MapsApiException("Google Geocoding: " + json.optString("status"));
			}
			JSONArray results = json.getJSONArray("results");
			if (results.length() == 0) {
				throw new MapsApiException("No se encontro la direccion: " + direccion);
			}
			JSONObject location = results.getJSONObject(0)
					.getJSONObject("geometry")
					.getJSONObject("location");
			return new double[] { location.getDouble("lat"), location.getDouble("lng") };
		} catch (MapsApiException e) {
			throw e;
		} catch (Exception e) {
			throw new MapsApiException("Error al geocodificar la direccion", e);
		}
	}

	public RutaRespuestaDTO calcularRutaPorDireccion(double originLat, double originLng, String destinoQuery) {
		double[] destCoords = obtenerCoordenadas(destinoQuery);
		return calcularRutaPorCoordenadas(originLat, originLng, destCoords[0], destCoords[1]);
	}

	public RutaRespuestaDTO calcularRutaPorCoordenadas(double originLat, double originLng, double destinoLat,
			double destinoLng) {
		try {
			String url = String.format(
					"https://maps.googleapis.com/maps/api/directions/json?origin=%s,%s&destination=%s,%s&mode=driving&key=%s",
					originLat, originLng, destinoLat, destinoLng, apiKey);

			String response = restClient.get()
					.uri(url)
					.retrieve()
					.body(String.class);

			JsonNode root = objectMapper.readTree(response);
			String status = root.path("status").asText();
			if (!"OK".equals(status)) {
				throw new MapsApiException("Google Directions: " + status);
			}

			JsonNode routes = root.path("routes");
			if (!routes.isArray() || routes.size() == 0) {
				throw new MapsApiException("No se encontro una ruta valida");
			}

			JsonNode route = routes.get(0);
			String polylinePoints = route.path("overview_polyline").path("points").asText();
			JsonNode leg = route.path("legs").get(0);
			double distanciaMetros = leg.path("distance").path("value").asDouble();
			double duracionSegundos = leg.path("duration").path("value").asDouble();

			return new RutaRespuestaDTO(polylinePoints, distanciaMetros / 1000.0, duracionSegundos / 60.0, 0.0,
					destinoLat, destinoLng);
		} catch (MapsApiException e) {
			throw e;
		} catch (Exception e) {
			throw new MapsApiException("Error al calcular la ruta en Google Directions", e);
		}
	}
}
