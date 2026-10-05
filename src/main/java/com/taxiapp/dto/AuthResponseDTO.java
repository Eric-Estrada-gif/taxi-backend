package com.taxiapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

	private String token;
	private Long usuarioId;
	private String nombre;
	private String email;
	private String rol;
	private String fotoPerfil;
	private Long conductorId;
}
