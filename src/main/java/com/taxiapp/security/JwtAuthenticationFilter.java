package com.taxiapp.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.taxiapp.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			String token = header.substring(7).trim();
			if (!token.isEmpty()) {
				try {
					Claims claims = jwtService.validarToken(token);
					Long userId = Long.valueOf(claims.getSubject());
					String email = claims.get("email", String.class);
					String rol = claims.get("rol", String.class);

					AuthPrincipal principal = new AuthPrincipal(userId, email, rol);
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							principal,
							null,
							List.of(new SimpleGrantedAuthority("ROLE_" + (rol != null ? rol : "ANONIMO"))));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				} catch (JwtException | IllegalArgumentException ex) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.getWriter().write("{\"error\":\"Token invalido o expirado\"}");
					return;
				}
			}
		}

		filterChain.doFilter(request, response);
	}
}
