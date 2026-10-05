package com.taxiapp.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;
	
	@Value("${jwt.expiration-ms}")
	private long expirationMs;
	
	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}
	
	public String generarToken(Long usuarioId, String email, String rol) {
		Date ahora = new Date();
		Date expiracion = new Date(ahora.getTime() + expirationMs);
		
		return Jwts.builder()
				.subject(String.valueOf(usuarioId))
				.claim("email", email)
				.claim("rol", rol)
				.issuedAt(ahora)
				.expiration(expiracion)
				.signWith(getKey())
				.compact();
	}
	
	// Valida el token y devuelve sus claims. Lanza JwtExc eption si el token
	// es invalido, esta mal firmado, o ya expiro
	public Claims validarToken(String token) {
		return Jwts.parser()
				.verifyWith(getKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
	
	public Long obtenerUsuarioId(String token) {
		Claims claims = validarToken(token);
		return Long.valueOf(claims.getSubject());
	}
}
