package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRegistroDTO {
	private String nombre;
	private String apellido;
	private String email;
	private String password;
	private String telefono;
	private String rol; // "RIDER", "CONDUCTOR" o "ADMIN"
}
