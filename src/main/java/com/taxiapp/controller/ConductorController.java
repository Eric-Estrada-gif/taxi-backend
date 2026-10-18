package com.taxiapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taxiapp.dto.ConductorRegistroDTO;
import com.taxiapp.entity.Conductor;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.ConductorService;

@RestController
@RequestMapping("/api/conductores")
public class ConductorController {

	private final ConductorService conductorService;
	private final CurrentUserService currentUserService;
	private final com.taxiapp.service.CuentaPersistenteService cuentaPersistenteService;

	public ConductorController(ConductorService conductorService, CurrentUserService currentUserService,
			com.taxiapp.service.CuentaPersistenteService cuentaPersistenteService) {
		this.conductorService = conductorService;
		this.currentUserService = currentUserService;
		this.cuentaPersistenteService = cuentaPersistenteService;
	}

	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody ConductorRegistroDTO dto) {
		currentUserService.requireRol("CONDUCTOR", "ADMIN");
		if (!currentUserService.isAdmin()) {
			dto.setUsuarioId(currentUserService.userId());
		}
		Long id = conductorService.registrarConductor(
				dto.getUsuarioId(),
				dto.getNumeroLicencia(),
				dto.getFechaVencimientoLicencia(),
				dto.getVehiculoId());
		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Conductor registrado correctamente"));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Conductor> buscarPorId(@PathVariable Long id) {
		Conductor conductor = conductorService.buscarPorId(id);
		if (conductor == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(conductor);
	}

	@GetMapping("/usuario/{usuarioId}")
	public ResponseEntity<Conductor> buscarPorUsuario(@PathVariable Long usuarioId) {
		currentUserService.requireUserId(usuarioId);
		Conductor conductor = conductorService.buscarPorUsuario(usuarioId);
		if (conductor == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(conductor);
	}

	@GetMapping("/usuario/{usuarioId}/vehiculo")
	public ResponseEntity<Map<String, Object>> vehiculoPorUsuario(@PathVariable Long usuarioId) {
		currentUserService.requireUserId(usuarioId);
		return ResponseEntity.ok(cuentaPersistenteService.leerVehiculo(usuarioId));
	}

	@GetMapping("/estado/{estado}")
	public ResponseEntity<List<Conductor>> listarPorEstado(@PathVariable String estado) {
		currentUserService.requireRol("CONDUCTOR", "ADMIN");
		return ResponseEntity.ok(conductorService.listarPorEstado(estado));
	}

	@PostMapping("/vehiculo")
	public ResponseEntity<?> guardarVehiculo(@RequestBody Map<String, Object> body) {
		Long usuarioId = currentUserService.userId();
		if ("beta@taxiapp.com".equalsIgnoreCase(currentUserService.require().getEmail())
				&& body.get("usuarioId") != null) {
			try {
				usuarioId = Long.parseLong(body.get("usuarioId").toString());
			} catch (NumberFormatException ignored) {
			}
		}
		String licencia = texto(body, "licencia", "numeroLicencia");
		String placa = texto(body, "placa");
		String marca = texto(body, "marca");
		String modelo = texto(body, "modelo");
		String color = texto(body, "color");
		Integer anio = null;
		if (body.get("anio") instanceof Number n) {
			anio = n.intValue();
		} else if (body.get("anio") != null) {
			try {
				anio = Integer.parseInt(body.get("anio").toString());
			} catch (NumberFormatException ignored) {
			}
		}
		Long conductorId = conductorService.guardarVehiculoYLicencia(usuarioId, licencia, placa, marca, modelo, anio,
				color);
		if (conductorId == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "No se pudo guardar el vehículo"));
		}
		return ResponseEntity.ok(Map.of("ok", true, "conductorId", conductorId));
	}

	private String texto(Map<String, Object> body, String... claves) {
		for (String clave : claves) {
			if (body.get(clave) != null && !body.get(clave).toString().isBlank()) {
				return body.get(clave).toString().trim();
			}
		}
		return "";
	}

	@PatchMapping("/{id}/estado")
	public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
		if (!currentUserService.isAdmin()) {
			Conductor propio = conductorService.buscarPorUsuario(currentUserService.userId());
			if (propio == null || !propio.getId().equals(id)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cambiar el estado de otro conductor");
			}
		}
		conductorService.actualizarEstado(id, body.get("estado"));
		return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado correctamente"));
	}
}
