package com.taxiapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.CalificacionRegistroDTO;
import com.taxiapp.entity.Calificacion;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.CalificacionService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionController {

	private final CalificacionService calificacionService;
	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;

	public CalificacionController(CalificacionService calificacionService, ViajeService viajeService,
			CurrentUserService currentUserService) {
		this.calificacionService = calificacionService;
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
	}

	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody CalificacionRegistroDTO dto) {
		viajeService.exigirParticipante(dto.getViajeId(), currentUserService.userId(), currentUserService.isAdmin());
		Long usuarioId = currentUserService.isAdmin() && dto.getUsuarioId() != null
				? dto.getUsuarioId()
				: currentUserService.userId();
		Long id = calificacionService.registrarCalificacion(
				dto.getViajeId(), usuarioId, dto.getPuntaje(), dto.getComentario());
		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Calificacion registrada correctamente"));
	}

	@GetMapping("/viaje/{viajeId}")
	public ResponseEntity<List<Calificacion>> listarPorViaje(@PathVariable Long viajeId) {
		viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
		return ResponseEntity.ok(calificacionService.listarPorViaje(viajeId));
	}

	@GetMapping("/conductor/{conductorUsuarioId}")
	public ResponseEntity<List<Calificacion>> listarRecibidasPorConductor(@PathVariable Long conductorUsuarioId) {
		currentUserService.requireUserId(conductorUsuarioId);
		return ResponseEntity.ok(calificacionService.listarRecibidasPorConductor(conductorUsuarioId));
	}
}
