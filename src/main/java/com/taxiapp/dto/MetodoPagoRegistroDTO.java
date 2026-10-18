package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetodoPagoRegistroDTO {

	private Long usuarioId;
	private String tipo;
	private String detalle;
	private Boolean predeterminado;
	private String numero;
	private String vence;
	private String cvv;
	private String yape;
}
