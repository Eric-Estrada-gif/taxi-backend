package com.taxiapp.dto;


import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UbicacionRegistroDTO {

	private Long viajeId;
	private BigDecimal lat;
	private BigDecimal lng;
}
