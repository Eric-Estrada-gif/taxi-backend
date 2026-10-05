package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequestDTO {
	
	private String idToken; //token que entrega el sdk de google en adroid
	private String rol; // "RIDER" o "CONDUCTOR" solo se usa si la cuenta es nueva
}
