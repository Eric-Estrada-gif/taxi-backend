package com.taxiapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.UsuarioRegistroDTO;
import com.taxiapp.entity.Usuario;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
	
	private final UsuarioService usuarioService;
	private final CurrentUserService currentUserService;
	
	@Autowired
	public UsuarioController(UsuarioService usuarioService, CurrentUserService currentUserService) {
		this.usuarioService = usuarioService;
		this.currentUserService = currentUserService;
	}
	
	//POST http://localhost:8080/api/usuarios
	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody UsuarioRegistroDTO dto) {
		Long id = usuarioService.registrarUsuario(
				dto.getNombre(),
				dto.getApellido(),
				dto.getEmail(),
				dto.getPassword(),
				dto.getTelefono(),
				dto.getRol()
				);
		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Usuario registrado correctamente"));
	}
	
	@PostMapping("/perfil")
	public ResponseEntity<?> guardarMiPerfil(@RequestBody Map<String, Object> body) {
		Long id = idParaPerfil(body);
		if (id == null || id <= 0) {
			return ResponseEntity.badRequest().body(Map.of("error", "No hay usuario para actualizar"));
		}
		return aplicarPerfil(id, body);
	}

	@PatchMapping("/{id}")
	@PutMapping("/{id}")
	@PostMapping("/{id}/perfil")
	public ResponseEntity<?> actualizarPerfil(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		return aplicarPerfil(id, body);
	}

	private ResponseEntity<?> aplicarPerfil(Long id, Map<String, Object> body) {
		currentUserService.requireUserId(id);
		String nombre = texto(body, "nombre");
		String apellido = texto(body, "apellido");
		String email = texto(body, "email");
		String telefono = texto(body, "telefono");
		String fotoPerfil = texto(body, "fotoPerfil");
		String sexo = texto(body, "sexo");
		String fechaNacimiento = texto(body, "fechaNacimiento");
		Usuario usuario = usuarioService.actualizarPerfil(id, nombre, apellido, email, telefono, fotoPerfil, sexo,
				fechaNacimiento);
		if (usuario == null) {
			return ResponseEntity.ok(Map.of("ok", true, "usuarioId", id));
		}
		usuario.setPassword(null);
		return ResponseEntity.ok(usuario);
	}

	private Long idParaPerfil(Map<String, Object> body) {
		try {
			Long sesion = currentUserService.userId();
			if (sesion != null && sesion > 0 && !"beta@taxiapp.com".equalsIgnoreCase(currentUserService.require().getEmail())) {
				return sesion;
			}
		} catch (Exception ignored) {
		}
		Object crudo = body.get("usuarioId");
		if (crudo instanceof Number n) {
			return n.longValue();
		}
		if (crudo != null) {
			try {
				return Long.parseLong(crudo.toString().trim());
			} catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	private String texto(Map<String, Object> body, String clave) {
		return body.get(clave) != null ? body.get(clave).toString() : null;
	}

	@GetMapping("/{id}")
	public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
		currentUserService.requireUserId(id);
		Usuario usuario = usuarioService.buscarPorId(id);
		
		if(usuario == null) {
			return ResponseEntity.notFound().build();
		}
		
		usuario.setPassword(null);
		return ResponseEntity.ok(usuario);
	}
	
	//GET http://localhost:8080/api/usuarios/email/eric@example.com
	@GetMapping("/email/{email}")
	public ResponseEntity<Usuario> buscarPorEmail(@PathVariable String email) {
		Usuario usuario = usuarioService.buscarPorEmail(email);
		
		if(usuario == null) {
			return ResponseEntity.notFound().build();
		}
		currentUserService.requireUserId(usuario.getId());
		usuario.setPassword(null);
		return ResponseEntity.ok(usuario);
	}
	
	//GET http://localhost:8080/api/usuarios/rol/RIDER
	@GetMapping("/rol/{rol}")
	public ResponseEntity<List<Usuario>> listarPorRol(@PathVariable String rol) {
		currentUserService.requireRol("ADMIN");
		List<Usuario> usuarios = usuarioService.listarPorRol(rol);
		usuarios.forEach(u -> u.setPassword(null));
		return ResponseEntity.ok(usuarios);
	}
}
