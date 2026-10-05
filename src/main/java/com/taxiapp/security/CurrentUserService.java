package com.taxiapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

	@Value("${app.security.beta-open:true}")
	private boolean betaOpen;

	public AuthPrincipal require() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
			return principal;
		}
		if (betaOpen) {
			return new AuthPrincipal(1L, "beta@taxiapp.com", "ADMIN");
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
	}

	public Long userId() {
		return require().getUserId();
	}

	public boolean isAdmin() {
		return require().tieneRol("ADMIN");
	}

	public boolean isBetaOpen() {
		return betaOpen;
	}

	public void requireRol(String... roles) {
		AuthPrincipal principal = require();
		for (String rol : roles) {
			if (principal.tieneRol(rol)) {
				return;
			}
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para esta operacion");
	}

	public void requireUserId(Long usuarioId) {
		if (isAdmin()) {
			return;
		}
		if (usuarioId == null || !usuarioId.equals(userId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes operar sobre otro usuario");
		}
	}
}
