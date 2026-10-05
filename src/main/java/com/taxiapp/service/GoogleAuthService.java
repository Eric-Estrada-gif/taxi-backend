package com.taxiapp.service;

import java.security.GeneralSecurityException;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.Collections;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.taxiapp.dto.AuthResponseDTO;
import com.taxiapp.dto.GoogleLoginRequestDTO;
import com.taxiapp.entity.Usuario;
import com.taxiapp.repository.UsuarioRepository;

@Service
public class GoogleAuthService {

	@Value("${google.oauth.client-id}")
	private String googleClientId;
	
	private final UsuarioRepository usuarioRepository;
	private JdbcTemplate jdbcTemplate;
	private final JwtService jwtService;
	private final DemoAccountService demoAccountService;

	@Autowired
	public GoogleAuthService(UsuarioRepository usuarioRepository, JdbcTemplate jdbcTemplate,
			JwtService jwtService, DemoAccountService demoAccountService) {
		this.usuarioRepository = usuarioRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.jwtService = jwtService;
		this.demoAccountService = demoAccountService;
	}
	
     //Excepcion especifica para cuando la cuenta existe con un rol distinto
     //al que se pidio en el login/registro.
	
	public static class RolConflictException extends RuntimeException {
		public RolConflictException(String message) {
			super(message);
		}
	}
	
	@Transactional
	public AuthResponseDTO loginConGoogle(GoogleLoginRequestDTO request) {
		
		GoogleIdToken.Payload payload = verificarToken(request.getIdToken());
		
		String googleId = payload.getSubject();
        String email = payload.getEmail();
        String nombre = (String) payload.get("given_name");
        String apellido = (String) payload.get("family_name");
        String fotoPerfil = (String) payload.get("picture");
 
        Usuario usuario = null;

        try {
            // Intenta buscar el usuario en la base de datos
            usuario = usuarioRepository.buscarPorGoogleId(googleId);
        } catch (Exception e) {
            // Si no existe y el SP lanza NoResultException, capturamos la excepción
            usuario = null;
        }

        if (usuario != null) {
            // La cuenta ya existe: el rol solicitado debe coincidir
            if (!usuario.getRol().name().equalsIgnoreCase(request.getRol())) {
                throw new RolConflictException(
                        "Esta cuenta ya esta registrada como " + usuario.getRol()
                                + ". Cambia el modo o usa otro correo de Google."
                );
            }
        } else {
            // Cuenta nueva: se crea automáticamente en MySQL
            Long nuevoId = registrarUsuarioGoogle(nombre, apellido, email, googleId, fotoPerfil, request.getRol());
            usuario = usuarioRepository.buscarPorGoogleId(googleId);
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
                conductorId
        );
    }
 
    private GoogleIdToken.Payload verificarToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
 
            GoogleIdToken idToken = verifier.verify(idTokenString);
 
            if (idToken == null) {
                throw new RuntimeException("Token de Google invalido o expirado");
            }
 
            return idToken.getPayload();
 
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw new RuntimeException("Error al verificar el token de Google: " + e.getMessage(), e);
        }
    }
 
    private Long registrarUsuarioGoogle(String nombre, String apellido, String email,
                                         String googleId, String fotoPerfil, String rol) {
 
        String sql = "{call sp_usuario_insertar_google(?, ?, ?, ?, ?, ?, ?)}";
 
        return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setString(1, nombre != null ? nombre : "");
                    cs.setString(2, apellido != null ? apellido : "");
                    cs.setString(3, email);
                    cs.setString(4, googleId);
                    cs.setString(5, fotoPerfil);
                    cs.setString(6, rol);
                    cs.registerOutParameter(7, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(7);
                });
    }
}
