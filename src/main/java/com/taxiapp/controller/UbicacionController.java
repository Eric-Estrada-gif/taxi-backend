package com.taxiapp.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taxiapp.dto.UbicacionRegistroDTO;
import com.taxiapp.entity.Ubicacion;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.UbicacionService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {

	private final UbicacionService ubicacionService;
	private final SimpMessagingTemplate simpMessagingTemplate;
	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;

	public UbicacionController(UbicacionService ubicacionService, SimpMessagingTemplate simpMessagingTemplate,
			ViajeService viajeService, CurrentUserService currentUserService) {
		this.ubicacionService = ubicacionService;
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
	}

	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody UbicacionRegistroDTO dto) {
		if (!currentUserService.isAdmin()
				&& !viajeService.esConductorUsuario(dto.getViajeId(), currentUserService.userId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el conductor asignado puede publicar GPS");
		}

		Long id = ubicacionService.registrarUbicacion(dto.getViajeId(), dto.getLat(), dto.getLng());

		Map<String, Object> payload = Map.of(
				"lat", dto.getLat(),
				"lng", dto.getLng());
		simpMessagingTemplate.convertAndSend((String) ("/topic/viajes/" + dto.getViajeId() + "/ubicacion"),
				(Object) payload);

		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Ubicacion registrada correctamente"));
	}

	@GetMapping("/viaje/{viajeId}")
	public ResponseEntity<?> listarPorViaje(@PathVariable Long viajeId) {
		viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
		return ResponseEntity.ok(ubicacionService.listarPorViaje(viajeId));
	}

	@GetMapping("/viaje/{viajeId}/ultima")
	public ResponseEntity<Ubicacion> obtenerUltima(@PathVariable Long viajeId) {
		viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
		Ubicacion ubicacion = ubicacionService.obtenerUltima(viajeId);
		if (ubicacion == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ubicacion);
	}
}
