package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetodoPagoRegistroDTO {

	private Long usuarioId;
	private String tipo; // TARJETA o BILLETERA_DIGITAL
	private String detalle;
	private Boolean predeterminado;
}
