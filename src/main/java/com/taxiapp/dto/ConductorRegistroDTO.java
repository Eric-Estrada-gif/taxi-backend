package com.taxiapp.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConductorRegistroDTO {

	private Long usuarioId;
	private String numeroLicencia;
	private LocalDate fechaVencimientoLicencia;
	private Long vehiculoId;
}
