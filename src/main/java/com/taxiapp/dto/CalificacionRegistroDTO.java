package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalificacionRegistroDTO {

	private Long viajeId;
	private Long usuarioId; // quien califica
	private Integer puntaje; // 1 - 5
	private String comentario;
}
