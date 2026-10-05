package com.taxiapp.security;

import java.security.Principal;
import java.util.Objects;

public class AuthPrincipal implements Principal {

	private final Long userId;
	private final String email;
	private final String rol;

	public AuthPrincipal(Long userId, String email, String rol) {
		this.userId = userId;
		this.email = email;
		this.rol = rol;
	}

	public Long getUserId() {
		return userId;
	}

	public String getEmail() {
		return email;
	}

	public String getRol() {
		return rol;
	}

	public boolean tieneRol(String esperado) {
		return esperado != null && esperado.equalsIgnoreCase(rol);
	}

	@Override
	public String getName() {
		return String.valueOf(userId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AuthPrincipal that)) {
			return false;
		}
		return Objects.equals(userId, that.userId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId);
	}
}
