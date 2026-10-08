package com.taxiapp.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.CallableStatement;
import java.sql.Types;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.taxiapp.dto.AuthResponseDTO;
import com.taxiapp.dto.FirebaseLoginRequestDTO;
import com.taxiapp.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Service
public class FirebaseAuthService {

	private static final String CERTS_URL =
			"https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

	private final JdbcTemplate jdbcTemplate;
	private final JwtService jwtService;
	private final DemoAccountService demoAccountService;
	private final PasswordEncoder passwordEncoder;
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final Map<String, PublicKey> clavesCache = new ConcurrentHashMap<>();
	private volatile Instant clavesHasta = Instant.EPOCH;

	@Value("${firebase.project-id:}")
	private String firebaseProjectId;

	public FirebaseAuthService(JdbcTemplate jdbcTemplate, JwtService jwtService,
			DemoAccountService demoAccountService, PasswordEncoder passwordEncoder) {
		this.jdbcTemplate = jdbcTemplate;
		this.jwtService = jwtService;
		this.demoAccountService = demoAccountService;
		this.passwordEncoder = passwordEncoder;
	}

	public static class RolConflictException extends RuntimeException {
		public RolConflictException(String message) {
			super(message);
		}
	}

	public static class PerfilRequeridoException extends RuntimeException {
		public PerfilRequeridoException(String message) {
			super(message);
		}
	}

	public Map<String, Object> consultarCuenta(String idToken) {
		Claims claims = verificarIdToken(idToken);
		String firebaseUid = claims.getSubject();
		String telefono = texto(claims.get("phone_number"));
		Usuario usuario = buscarPorFirebase(firebaseUid, telefono);
		if (usuario == null) {
			return Map.of(
					"existe", false,
					"telefono", telefono == null ? "" : telefono);
		}
		return Map.of(
				"existe", true,
				"nombre", nvl(usuario.getNombre()),
				"apellido", nvl(usuario.getApellido()),
				"email", nvl(usuario.getEmail()),
				"rol", usuario.getRol() == null ? "" : usuario.getRol().name(),
				"telefono", nvl(usuario.getTelefono()));
	}

	@Transactional
	public AuthResponseDTO loginConFirebase(FirebaseLoginRequestDTO request) {
		Claims claims = verificarIdToken(request.getIdToken());
		String firebaseUid = claims.getSubject();
		String telefonoToken = texto(claims.get("phone_number"));
		String telefono = primeroNoVacio(request.getTelefono(), telefonoToken);
		String googleId = "fb:" + firebaseUid;

		Usuario usuario = buscarPorFirebase(firebaseUid, telefono);
		if (usuario != null) {
			if (request.getRol() != null && !request.getRol().isBlank()
					&& !usuario.getRol().name().equalsIgnoreCase(request.getRol())) {
				throw new RolConflictException(
						"Este número ya está registrado como " + usuario.getRol()
								+ ". Cambia el modo o usa otro número.");
			}
			if (telefono != null && !telefono.isBlank()
					&& (usuario.getTelefono() == null || usuario.getTelefono().isBlank())) {
				jdbcTemplate.update("UPDATE usuario SET telefono = ? WHERE id = ?", telefono, usuario.getId());
			}
		} else {
			String nombre = trim(request.getNombre());
			String apellido = trim(request.getApellido());
			if (nombre.isBlank() || apellido.isBlank()) {
				throw new PerfilRequeridoException("Completa nombre y apellido para registrarte con este número.");
			}
			String rol = request.getRol() == null ? "RIDER" : request.getRol().trim().toUpperCase();
			if (!rol.equals("RIDER") && !rol.equals("CONDUCTOR")) {
				rol = "RIDER";
			}
			String email = emailDeRegistro(request.getEmail(), telefono, firebaseUid);
			registrarUsuario(nombre, apellido, email, googleId, telefono, rol);
			usuario = buscarPorFirebase(firebaseUid, telefono);
			if (usuario == null) {
				throw new IllegalStateException("No se pudo crear el usuario de celular");
			}
		}

		String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name());
		Long conductorId = null;
		if ("CONDUCTOR".equalsIgnoreCase(usuario.getRol().name())) {
			demoAccountService.asegurarPerfilConductor(usuario.getId());
			conductorId = demoAccountService.conductorIdDeUsuario(usuario.getId());
		}

		return new AuthResponseDTO(
				token,
				usuario.getId(),
				usuario.getNombre(),
				usuario.getEmail(),
				usuario.getRol().name(),
				usuario.getFotoPerfil(),
				conductorId);
	}

	private Claims verificarIdToken(String idToken) {
		if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
			throw new IllegalStateException("Falta FIREBASE_PROJECT_ID en el backend");
		}
		if (idToken == null || idToken.isBlank()) {
			throw new IllegalArgumentException("Falta el idToken de Firebase");
		}
		JSONObject header = jsonDeParte(idToken, 0);
		String kid = header.optString("kid", "");
		if (kid.isBlank()) {
			throw new IllegalArgumentException("Token de Firebase sin kid");
		}
		PublicKey clave = clavePublica(kid);
		Claims claims = Jwts.parser()
				.verifyWith(clave)
				.requireIssuer("https://securetoken.google.com/" + firebaseProjectId)
				.requireAudience(firebaseProjectId)
				.clockSkewSeconds(60)
				.build()
				.parseSignedClaims(idToken)
				.getPayload();
		if (!"https://securetoken.google.com/".concat(firebaseProjectId).equals(claims.getIssuer())) {
			throw new IllegalArgumentException("Issuer de Firebase inválido");
		}
		return claims;
	}

	private PublicKey clavePublica(String kid) {
		recargarClavesSiHaceFalta();
		PublicKey clave = clavesCache.get(kid);
		if (clave == null) {
			clavesHasta = Instant.EPOCH;
			recargarClavesSiHaceFalta();
			clave = clavesCache.get(kid);
		}
		if (clave == null) {
			throw new IllegalArgumentException("No hay certificado Firebase para kid " + kid);
		}
		return clave;
	}

	private synchronized void recargarClavesSiHaceFalta() {
		if (Instant.now().isBefore(clavesHasta) && !clavesCache.isEmpty()) {
			return;
		}
		String cuerpo;
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(CERTS_URL)).GET().build();
			cuerpo = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
		} catch (Exception e) {
			throw new IllegalStateException("No se pudieron descargar los certificados de Firebase", e);
		}
		if (cuerpo == null || cuerpo.isBlank()) {
			throw new IllegalStateException("No se pudieron descargar los certificados de Firebase");
		}
		JSONObject json = new JSONObject(cuerpo);
		clavesCache.clear();
		for (String kid : json.keySet()) {
			clavesCache.put(kid, publicKeyDePem(json.getString(kid)));
		}
		clavesHasta = Instant.now().plusSeconds(3600);
	}

	private PublicKey publicKeyDePem(String pem) {
		try {
			String limpio = pem.replace("-----BEGIN CERTIFICATE-----", "")
					.replace("-----END CERTIFICATE-----", "")
					.replaceAll("\\s", "");
			byte[] der = Base64.getDecoder().decode(limpio);
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
			return cert.getPublicKey();
		} catch (Exception e) {
			throw new IllegalStateException("Certificado Firebase inválido", e);
		}
	}

	private JSONObject jsonDeParte(String jwt, int indice) {
		String[] partes = jwt.split("\\.");
		if (partes.length < 2) {
			throw new IllegalArgumentException("Token de Firebase inválido");
		}
		byte[] bytes = Base64.getUrlDecoder().decode(partes[indice]);
		return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
	}

	private Usuario buscarPorFirebase(String firebaseUid, String telefono) {
		Usuario porUid = buscarUno("SELECT id, nombre, apellido, email, rol, foto_perfil, telefono FROM usuario WHERE google_id = ? LIMIT 1",
				"fb:" + firebaseUid);
		if (porUid != null) {
			return porUid;
		}
		if (telefono == null || telefono.isBlank()) {
			return null;
		}
		return buscarUno("SELECT id, nombre, apellido, email, rol, foto_perfil, telefono FROM usuario WHERE telefono = ? LIMIT 1",
				telefono);
	}

	private Usuario buscarUno(String sql, String valor) {
		try {
			return jdbcTemplate.query(sql, rs -> {
				if (!rs.next()) {
					return null;
				}
				Usuario usuario = new Usuario();
				usuario.setId(rs.getLong("id"));
				usuario.setNombre(rs.getString("nombre"));
				usuario.setApellido(rs.getString("apellido"));
				usuario.setEmail(rs.getString("email"));
				usuario.setFotoPerfil(rs.getString("foto_perfil"));
				usuario.setTelefono(rs.getString("telefono"));
				String rol = rs.getString("rol");
				if (rol != null) {
					usuario.setRol(Usuario.Rol.valueOf(rol));
				}
				return usuario;
			}, valor);
		} catch (Exception e) {
			return null;
		}
	}

	private void registrarUsuario(String nombre, String apellido, String email, String googleId,
			String telefono, String rol) {
		try {
			String sql = "{call sp_usuario_insertar_google(?, ?, ?, ?, ?, ?, ?)}";
			jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
					(CallableStatement cs) -> {
						cs.setString(1, nombre);
						cs.setString(2, apellido);
						cs.setString(3, email);
						cs.setString(4, googleId);
						cs.setString(5, null);
						cs.setString(6, rol);
						cs.registerOutParameter(7, Types.BIGINT);
						cs.execute();
						return cs.getLong(7);
					});
		} catch (Exception e) {
			jdbcTemplate.update(
					"""
							INSERT INTO usuario (nombre, apellido, email, password, telefono, rol, activo, google_id)
							VALUES (?, ?, ?, ?, ?, ?, 1, ?)
							""",
					nombre, apellido, email, passwordEncoder.encode("firebase-phone"), telefono, rol, googleId);
		}
		if (telefono != null && !telefono.isBlank()) {
			jdbcTemplate.update("UPDATE usuario SET telefono = ? WHERE google_id = ?", telefono, googleId);
		}
	}

	private String emailDeRegistro(String emailPedido, String telefono, String uid) {
		String email = trim(emailPedido);
		if (!email.isBlank()) {
			return email;
		}
		String digits = (telefono == null ? "" : telefono).replaceAll("\\D", "");
		if (digits.isBlank()) {
			digits = uid.replaceAll("[^a-zA-Z0-9]", "");
		}
		return digits + "@phone.rapitrip.local";
	}

	private String texto(Object valor) {
		return valor == null ? null : String.valueOf(valor);
	}

	private String trim(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private String nvl(String valor) {
		return valor == null ? "" : valor;
	}

	private String primeroNoVacio(String a, String b) {
		if (a != null && !a.isBlank()) {
			return a.trim();
		}
		if (b != null && !b.isBlank()) {
			return b.trim();
		}
		return null;
	}
}
