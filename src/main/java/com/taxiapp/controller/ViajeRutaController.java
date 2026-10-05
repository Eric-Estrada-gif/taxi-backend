package com.taxiapp.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taxiapp.dto.EstimacionRequestDTO;
import com.taxiapp.dto.EstimacionResponseDTO;
import com.taxiapp.dto.RutaRespuestaDTO;
import com.taxiapp.service.EstimacionService;
import com.taxiapp.service.GoogleDirectionsService;
import com.taxiapp.service.GoogleMapsService;

@RestController
@RequestMapping("/api/viajes")
public class ViajeRutaController {

	private final GoogleDirectionsService rutaService;
	private final GoogleMapsService googleMapsService;
	private final EstimacionService estimacionService;

	public ViajeRutaController(GoogleDirectionsService rutaService, GoogleMapsService googleMapsService,
			EstimacionService estimacionService) {
		this.rutaService = rutaService;
		this.googleMapsService = googleMapsService;
		this.estimacionService = estimacionService;
	}

	@GetMapping({ "/ruta", "/calcular-ruta" })
	public ResponseEntity<RutaRespuestaDTO> obtenerRuta(
			@RequestParam(required = false) Double originLat,
			@RequestParam(required = false) Double originLng,
			@RequestParam(required = false) Double origenLat,
			@RequestParam(required = false) Double origenLng,
			@RequestParam(required = false) String destinoQuery,
			@RequestParam(required = false) Double destinoLat,
			@RequestParam(required = false) Double destinoLng) {

		double oLat = primerValor(originLat, origenLat);
		double oLng = primerValor(originLng, origenLng);

		RutaRespuestaDTO resultado;
		double destLat;
		double destLng;

		if (destinoLat != null && destinoLng != null) {
			destLat = destinoLat;
			destLng = destinoLng;
			resultado = rutaService.calcularRutaPorCoordenadas(oLat, oLng, destLat, destLng);
		} else {
			if (destinoQuery == null || destinoQuery.isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta destinoQuery o destinoLat/destinoLng");
			}
			double[] coords = rutaService.obtenerCoordenadas(destinoQuery);
			destLat = coords[0];
			destLng = coords[1];
			resultado = rutaService.calcularRutaPorCoordenadas(oLat, oLng, destLat, destLng);
		}

		resultado.setDestinoLat(destLat);
		resultado.setDestinoLng(destLng);

		EstimacionRequestDTO request = new EstimacionRequestDTO();
		request.setOrigenLat(BigDecimal.valueOf(oLat));
		request.setOrigenLng(BigDecimal.valueOf(oLng));
		request.setDestinoLat(BigDecimal.valueOf(destLat));
		request.setDestinoLng(BigDecimal.valueOf(destLng));

		List<EstimacionResponseDTO> estimaciones = estimacionService.estimar(request);
		estimaciones.stream()
				.min(Comparator.comparing(EstimacionResponseDTO::getPrecioEstimado))
				.ifPresent(min -> resultado.setPrecioEstimado(min.getPrecioEstimado().doubleValue()));

		return ResponseEntity.ok(resultado);
	}

	@GetMapping("/sugerencias")
	public ResponseEntity<List<GoogleMapsService.LugarSugerenciaDTO>> obtenerSugerencias(@RequestParam String query) {
		return ResponseEntity.ok(googleMapsService.obtenerSugerencias(query));
	}

	private double primerValor(Double a, Double b) {
		if (a != null) {
			return a;
		}
		if (b != null) {
			return b;
		}
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta originLat/origenLat y originLng/origenLng");
	}
}
