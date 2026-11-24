package com.taxiapp.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RutaRespuestaDTO {

	private String polylinePoints;
	private double distanciaKM;
	private double duracionMinutos;
	private double precioEstimado;
	private Double destinoLat;
	private Double destinoLng;
	private List<PasoRutaDTO> pasos = new ArrayList<>();
}
