package com.taxiapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasoRutaDTO {

	private String instruction;
	private String maneuver;
	private int distanceMeters;
	private int durationSeconds;
	private Double endLat;
	private Double endLng;
}
