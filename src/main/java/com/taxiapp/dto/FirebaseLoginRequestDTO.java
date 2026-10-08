package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirebaseLoginRequestDTO {

	private String idToken;
	private String rol;
	private String nombre;
	private String apellido;
	private String email;
	private String telefono;
}
