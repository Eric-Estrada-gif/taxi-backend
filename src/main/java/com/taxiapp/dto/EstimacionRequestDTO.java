package com.taxiapp.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimacionRequestDTO {

	private BigDecimal origenLat;
	private BigDecimal origenLng;
	private BigDecimal destinoLat;
	private BigDecimal destinoLng;
}
