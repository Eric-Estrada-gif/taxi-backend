package com.taxiapp.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Una zona de alerta es un círculo. El punto seguro sale del círculo
 * caminando como mucho 300 metros desde el pin original.
 */
@Service
public class ZonaAlertaService {

	public static final int CAMINATA_MAX_METROS = 300;

	private final JdbcTemplate jdbcTemplate;

	public ZonaAlertaService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public boolean enZona(double lat, double lng) {
		return zonaQueContiene(lat, lng) != null;
	}

	/**
	 * Punto justo afuera de todas las zonas, a no más de 300 m del pin.
	 * Null si el pin está tan adentro que no se sale caminando esa distancia.
	 */
	public double[] puntoSeguro(double lat, double lng) {
		Circulo dentro = zonaQueContiene(lat, lng);
		if (dentro == null) {
			return null;
		}
		double rumbo = distanciaMetros(dentro.lat, dentro.lng, lat, lng) < 8
				? 90
				: rumboGrados(dentro.lat, dentro.lng, lat, lng);
		for (int metros = 20; metros <= CAMINATA_MAX_METROS; metros += 20) {
			double[] candidato = desplazar(lat, lng, rumbo, metros);
			if (zonaQueContiene(candidato[0], candidato[1]) == null) {
				return candidato;
			}
		}
		return null;
	}

	public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
		double r = 6_371_000.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * r * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	private Circulo zonaQueContiene(double lat, double lng) {
		List<Circulo> zonas = jdbcTemplate.query(
				"SELECT lat, lng, radio_metros FROM zona_alerta WHERE activa = 1",
				(rs, rowNum) -> new Circulo(rs.getDouble("lat"), rs.getDouble("lng"), rs.getInt("radio_metros")));
		Circulo mejor = null;
		double menorHolgura = Double.MAX_VALUE;
		for (Circulo zona : zonas) {
			double distancia = distanciaMetros(lat, lng, zona.lat, zona.lng);
			double holgura = zona.radio - distancia;
			if (holgura >= 0 && holgura < menorHolgura) {
				menorHolgura = holgura;
				mejor = zona;
			}
		}
		return mejor;
	}

	private static double rumboGrados(double lat1, double lng1, double lat2, double lng2) {
		double y = Math.sin(Math.toRadians(lng2 - lng1)) * Math.cos(Math.toRadians(lat2));
		double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
				- Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(lng2 - lng1));
		return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
	}

	private static double[] desplazar(double lat, double lng, double rumboGrados, double metros) {
		double r = 6_371_000.0;
		double br = Math.toRadians(rumboGrados);
		double lat1 = Math.toRadians(lat);
		double lng1 = Math.toRadians(lng);
		double ang = metros / r;
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(ang) + Math.cos(lat1) * Math.sin(ang) * Math.cos(br));
		double lng2 = lng1 + Math.atan2(
				Math.sin(br) * Math.sin(ang) * Math.cos(lat1),
				Math.cos(ang) - Math.sin(lat1) * Math.sin(lat2));
		return new double[] { Math.toDegrees(lat2), Math.toDegrees(lng2) };
	}

	private record Circulo(double lat, double lng, int radio) {
	}
}
