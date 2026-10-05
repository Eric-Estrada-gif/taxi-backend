package com.taxiapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
	
	//GET http://localhost:8080/api/usuarios/1
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
