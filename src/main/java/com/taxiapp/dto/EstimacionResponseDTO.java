package com.taxiapp.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EstimacionResponseDTO {

	private Long tarifaId;
	private String nombreCategoria;
	private BigDecimal precioEstimado;
	private double distanciaKm;
	private double duracionMinutos;
}
