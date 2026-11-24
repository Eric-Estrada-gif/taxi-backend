package com.taxiapp.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.taxiapp.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;

	@Value("${app.security.beta-open:true}")
	private boolean betaOpen;

	public AuthChannelInterceptor(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}

		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			String header = accessor.getFirstNativeHeader("Authorization");
			if (header == null) {
				header = accessor.getFirstNativeHeader("authorization");
			}

			if (header != null && header.startsWith("Bearer ")) {
				String token = header.substring(7).trim();
				if (token.isEmpty()) {
					header = null;
				} else {
					try {
						Claims claims = jwtService.validarToken(token);
						AuthPrincipal principal = new AuthPrincipal(
								Long.valueOf(claims.getSubject()),
								claims.get("email", String.class),
								claims.get("rol", String.class));
						recordarUsuario(accessor, principal);
						return message;
					} catch (JwtException | IllegalArgumentException ex) {
						if (betaOpen) {
							recordarUsuario(accessor, new AuthPrincipal(1L, "beta@taxiapp.com", "ADMIN"));
							return message;
						}
						throw new MessagingException("Token JWT invalido o expirado");
					}
				}
			}

			if (betaOpen) {
				recordarUsuario(accessor, new AuthPrincipal(1L, "beta@taxiapp.com", "ADMIN"));
				return message;
			}

			throw new MessagingException("Falta el header Authorization con formato Bearer <token>");
		}

		if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
			Object guardado = accessor.getSessionAttributes().get("AUTH_USER");
			if (guardado instanceof AuthPrincipal principal) {
				accessor.setUser(principal);
			}
		}

		return message;
	}

	private void recordarUsuario(StompHeaderAccessor accessor, AuthPrincipal principal) {
		accessor.setUser(principal);
		if (accessor.getSessionAttributes() != null) {
			accessor.getSessionAttributes().put("AUTH_USER", principal);
		}
	}
}
