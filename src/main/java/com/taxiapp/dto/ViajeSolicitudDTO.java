package com.taxiapp.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViajeSolicitudDTO {

	private Long riderId;
	private Long tarifaId; //categoria elegida
	private String origenDireccion;
	private BigDecimal origenLat;
	private BigDecimal origenLng;
	private String destinoDireccion;
	private BigDecimal destinoLat;
	private BigDecimal destinoLng;
	private BigDecimal montoTotal; // precio que se le muestra al rider en la estimacion
}
