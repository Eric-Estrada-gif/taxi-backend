package com.taxiapp.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagoRegistroDTO {

	private Long viajeId;
	private String metodo; // efectivo, tarjeta, billetera digital
	private BigDecimal monto;
}
