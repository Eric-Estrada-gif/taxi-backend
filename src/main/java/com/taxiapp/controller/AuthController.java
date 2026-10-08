package com.taxiapp.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.AuthResponseDTO;
import com.taxiapp.dto.DemoLoginRequestDTO;
import com.taxiapp.dto.FirebaseLoginRequestDTO;
import com.taxiapp.dto.GoogleLoginRequestDTO;
import com.taxiapp.entity.Usuario;
import com.taxiapp.service.DemoAccountService;
import com.taxiapp.service.FirebaseAuthService;
import com.taxiapp.service.GoogleAuthService;
import com.taxiapp.service.JwtService;
import com.taxiapp.service.UsuarioService;

import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final GoogleAuthService googleAuthService;
	private final FirebaseAuthService firebaseAuthService;
	private final JwtService jwtService;
	private final DemoAccountService demoAccountService;
	private final UsuarioService usuarioService;

	@Autowired
	public AuthController(GoogleAuthService googleAuthService, FirebaseAuthService firebaseAuthService,
			JwtService jwtService, DemoAccountService demoAccountService, UsuarioService usuarioService) {
		this.googleAuthService = googleAuthService;
		this.firebaseAuthService = firebaseAuthService;
		this.jwtService = jwtService;
		this.demoAccountService = demoAccountService;
		this.usuarioService = usuarioService;
	}

	@GetMapping("/demo")
	public ResponseEntity<?> loginDemoGet(@RequestParam(defaultValue = "RIDER") String rol) {
		return emitirSesionDemo(rol);
	}

	@PostMapping("/demo")
	public ResponseEntity<?> loginDemo(@RequestBody(required = false) DemoLoginRequestDTO body) {
		String rolPedido = body != null && body.getRol() != null ? body.getRol() : "RIDER";
		return emitirSesionDemo(rolPedido);
	}

	private ResponseEntity<?> emitirSesionDemo(String rolCrudo) {
		String rolPedido = rolCrudo == null ? "RIDER" : rolCrudo.trim().toUpperCase();
		if (!rolPedido.equals("RIDER") && !rolPedido.equals("CONDUCTOR") && !rolPedido.equals("ADMIN")) {
			return ResponseEntity.badRequest().body(Map.of("error", "rol debe ser RIDER, CONDUCTOR o ADMIN"));
		}

		try {
			Usuario usuario = demoAccountService.asegurarUsuarioConRol(rolPedido);
			String token = jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol().name());
			Long conductorId = null;
			if ("CONDUCTOR".equalsIgnoreCase(usuario.getRol().name())) {
				conductorId = demoAccountService.conductorIdDeUsuario(usuario.getId());
			}

			Map<String, Object> respuesta = new java.util.LinkedHashMap<>();
			respuesta.put("token", token);
			respuesta.put("usuarioId", usuario.getId());
			respuesta.put("nombre", usuario.getNombre());
			respuesta.put("email", usuario.getEmail());
			respuesta.put("rol", usuario.getRol().name());
			respuesta.put("fotoPerfil", usuario.getFotoPerfil());
			respuesta.put("conductorId", conductorId);
			return ResponseEntity.ok(respuesta);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "No se pudo crear la sesion demo: " + e.getMessage()));
		}
	}

	// POST http://localhost:8080/api/auth/google
    // body: { "idToken": "...", "rol": "RIDER" }
	// Login por celular (Firebase Phone). Railway debe desplegar este endpoint.
	@PostMapping("/firebase/check")
	public ResponseEntity<?> consultarFirebase(@RequestBody FirebaseLoginRequestDTO request) {
		try {
			return ResponseEntity.ok(firebaseAuthService.consultarCuenta(request.getIdToken()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "No se pudo verificar el celular: " + e.getMessage()));
		}
	}

	@PostMapping("/firebase")
	public ResponseEntity<?> loginConFirebase(@RequestBody FirebaseLoginRequestDTO request) {
		try {
			AuthResponseDTO respuesta = firebaseAuthService.loginConFirebase(request);
			return ResponseEntity.ok(respuesta);
		} catch (FirebaseAuthService.RolConflictException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		} catch (FirebaseAuthService.PerfilRequeridoException e) {
			return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "No se pudo autenticar el celular: " + e.getMessage()));
		}
	}

	@PostMapping("/google")
	public ResponseEntity<?> loginConGoogle(@RequestBody GoogleLoginRequestDTO request) {
		try {
			AuthResponseDTO respuesta = googleAuthService.loginConGoogle(request);
			return ResponseEntity.ok(respuesta);
			
		} catch (GoogleAuthService.RolConflictException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
			
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "No se pudo autenticar: " + e.getMessage()));
		}
	}
	
	// GET http://localhost:8080/api/auth/me
	// Header: Authorization: Bearer <token>
	@GetMapping("/me")
	public ResponseEntity<?> obtenerUsuarioActual(@RequestHeader("Authorization") String authHeader) {
		try {
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Falta el header Authorization con formato 'Bearer <token>'"));
			}
			
			String token = authHeader.substring(7).trim();
			Long usaurioId = jwtService.obtenerUsuarioId(token);
			
			Usuario usuario = usuarioService.buscarPorId(usaurioId);
			
			if (usuario == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
			}
			
			return ResponseEntity.ok(usuario);
		} catch (JwtException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token invalido o expirado"));
		}
	}
}
