package com.taxiapp.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehiculoRegistradoDTO {
	
	private String placa;
	private String marca;
	private String modelo;
	private Integer anio;
	private String color;
	private LocalDate soatVigenteHasta; // formato JSON: "2027-05-20"
}
