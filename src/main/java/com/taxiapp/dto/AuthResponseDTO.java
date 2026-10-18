package com.taxiapp.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponseDTO {

	private String token;
	private Long usuarioId;
	private String nombre;
	private String apellido;
	private String email;
	private String telefono;
	private String rol;
	private String fotoPerfil;
	private Long conductorId;
	private Boolean cuentaNueva;
	private String metodoPago;
	private String pagoNumero;
	private String pagoVence;
	private String pagoCvv;
	private String pagoYape;
	private String licenciaNumero;
	private String vehiculoPlaca;
	private String vehiculoMarca;
	private String vehiculoModelo;
	private String vehiculoColor;
	private String vehiculoAnio;

	public static AuthResponseDTO de(String token, Long usuarioId, String nombre, String apellido,
			String email, String telefono, String rol, String fotoPerfil, Long conductorId, Boolean cuentaNueva) {
		AuthResponseDTO dto = new AuthResponseDTO();
		dto.setToken(token);
		dto.setUsuarioId(usuarioId);
		dto.setNombre(nombre);
		dto.setApellido(apellido);
		dto.setEmail(email);
		dto.setTelefono(telefono);
		dto.setRol(rol);
		dto.setFotoPerfil(fotoPerfil);
		dto.setConductorId(conductorId);
		dto.setCuentaNueva(cuentaNueva);
		return dto;
	}
}
