package com.taxiapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TarifaRegistroDTO {

	private String nombre;
	private BigDecimal tarifaBase;
	private BigDecimal precioKm;
	private BigDecimal precioMinuto;
	private LocalDate vigenteDesde; //formato JSON: "2026-09-13"
}
