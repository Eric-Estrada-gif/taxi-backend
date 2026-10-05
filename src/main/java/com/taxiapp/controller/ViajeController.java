package com.taxiapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.taxiapp.dto.ViajeHistorialDTO;
import com.taxiapp.dto.ViajeSolicitudDTO;
import com.taxiapp.entity.Conductor;
import com.taxiapp.entity.Ubicacion;
import com.taxiapp.entity.Viaje;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.ConductorService;
import com.taxiapp.service.FcmService;
import com.taxiapp.service.UbicacionService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

	private final ViajeService viajeService;
	private final SimpMessagingTemplate simpMessagingTemplate;
	private final CurrentUserService currentUserService;
	private final ConductorService conductorService;
	private final UbicacionService ubicacionService;
	private final FcmService fcmService;

	public ViajeController(ViajeService viajeService, SimpMessagingTemplate simpMessagingTemplate,
			CurrentUserService currentUserService, ConductorService conductorService,
			UbicacionService ubicacionService, FcmService fcmService) {
		this.viajeService = viajeService;
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.currentUserService = currentUserService;
		this.conductorService = conductorService;
		this.ubicacionService = ubicacionService;
		this.fcmService = fcmService;
	}

	private void notificarCambioEstado(Long viajeId, String estado, Long conductorId) {
		Map<String, Object> payload = viajeService.armarPayloadEstado(viajeId, estado, conductorId);
		simpMessagingTemplate.convertAndSend((String) ("/topic/viajes/" + viajeId + "/estado"), (Object) payload);
		simpMessagingTemplate.convertAndSend((String) ("/topic/viajes/" + viajeId), (Object) payload);
	}

	@PostMapping
	public ResponseEntity<?> solicitar(@RequestBody ViajeSolicitudDTO dto) {
		currentUserService.requireRol("RIDER", "ADMIN");
		if (!currentUserService.isAdmin()) {
			dto.setRiderId(currentUserService.userId());
		}
		if (dto.getRiderId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta riderId");
		}

		Long id = viajeService.solicitarViaje(dto);

		Map<String, Object> payload = new HashMap<>();
		payload.put("viajeId", id);
		payload.put("estado", "SOLICITADO");
		payload.put("origenDireccion", dto.getOrigenDireccion());
		payload.put("destinoDireccion", dto.getDestinoDireccion());
		payload.put("origenLat", dto.getOrigenLat());
		payload.put("origenLng", dto.getOrigenLng());
		payload.put("destinoLat", dto.getDestinoLat());
		payload.put("destinoLng", dto.getDestinoLng());
		payload.put("montoTotal", dto.getMontoTotal());

		simpMessagingTemplate.convertAndSend((String) ("/topic/viajes/" + id + "/estado"), (Object) payload);
		simpMessagingTemplate.convertAndSend((String) "/topic/viajes/solicitados", (Object) payload);

		fcmService.notificarConductoresDisponibles(id, dto.getOrigenDireccion(), dto.getDestinoDireccion(),
				dto.getMontoTotal());

		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Viaje solicitado correctamente"));
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
		if (!currentUserService.isBetaOpen()) {
			viajeService.exigirParticipante(id, currentUserService.userId(), currentUserService.isAdmin());
		}

		Map<String, Object> response = viajeService.obtenerDetalleParaCliente(id);
		if (response == null) {
			return ResponseEntity.notFound().build();
		}

		Object estado = response.get("estado");
		if (estado != null && !(estado instanceof String)) {
			response.put("estado", estado.toString());
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/estado")
	public ResponseEntity<?> obtenerEstado(@PathVariable Long id) {
		String estado = viajeService.obtenerEstado(id);
		if (estado == null) {
			return ResponseEntity.notFound().build();
		}
		Map<String, Object> response = new HashMap<>();
		response.put("id", id);
		response.put("viajeId", id);
		response.put("estado", estado);
		response.put("conductorId", viajeService.obtenerConductorId(id));
		return ResponseEntity.ok(response);
	}

	@GetMapping("/rider/{riderId}")
	public ResponseEntity<List<ViajeHistorialDTO>> listarPorRider(@PathVariable Long riderId) {
		currentUserService.requireUserId(riderId);
		return ResponseEntity.ok(viajeService.listarHistorialPorRiderDTO(riderId));
	}

	@GetMapping("/conductor/{conductorId}")
	public ResponseEntity<List<ViajeHistorialDTO>> listarPorConductor(@PathVariable Long conductorId) {
		if (!currentUserService.isAdmin()) {
			currentUserService.requireRol("CONDUCTOR");
			Conductor propio = conductorDelUsuarioActual();
			if (!propio.getId().equals(conductorId)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes ver viajes de otro conductor");
			}
		}
		return ResponseEntity.ok(viajeService.listarHistorialPorConductorDTO(conductorId));
	}

	@GetMapping("/solicitados")
	public ResponseEntity<List<Viaje>> listarSolicitados() {
		currentUserService.requireRol("CONDUCTOR", "ADMIN");
		return ResponseEntity.ok(viajeService.listarSolicitados());
	}

	@PatchMapping("/{id}/aceptar")
	public ResponseEntity<?> aceptar(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
		currentUserService.requireRol("CONDUCTOR", "ADMIN");
		Long conductorId;
		if (body != null && body.get("conductorId") instanceof Number raw) {
			conductorId = raw.longValue();
			if (!currentUserService.isAdmin()) {
				Conductor propio = conductorDelUsuarioActual();
				if (!propio.getId().equals(conductorId)) {
					throw new ResponseStatusException(HttpStatus.FORBIDDEN, "conductorId no coincide con tu sesion");
				}
			}
		} else {
			conductorId = conductorDelUsuarioActual().getId();
		}
		viajeService.aceptar(id, conductorId);
		notificarCambioEstado(id, "ACEPTADO", conductorId);
		return ResponseEntity.ok(Map.of("mensaje", "Viaje aceptado correctamente", "conductorId", conductorId));
	}

	@PatchMapping("/{id}/iniciar")
	public ResponseEntity<?> iniciar(@PathVariable Long id) {
		exigirConductorAsignado(id);
		viajeService.iniciar(id);
		notificarCambioEstado(id, "EN_CURSO", viajeService.obtenerConductorId(id));
		return ResponseEntity.ok(Map.of("mensaje", "Viaje iniciado correctamente"));
	}

	@PatchMapping("/{id}/finalizar")
	public ResponseEntity<?> finalizar(@PathVariable Long id) {
		exigirConductorAsignado(id);
		viajeService.finalizar(id);
		notificarCambioEstado(id, "FINALIZADO", viajeService.obtenerConductorId(id));
		return ResponseEntity.ok(Map.of("mensaje", "Viaje finalizado correctamente"));
	}

	@PatchMapping("/{id}/cancelar")
	public ResponseEntity<?> cancelar(@PathVariable Long id) {
		Long usuarioId = currentUserService.userId();
		boolean rider = viajeService.esRider(id, usuarioId);
		boolean conductor = viajeService.esConductorUsuario(id, usuarioId);
		if (!currentUserService.isAdmin() && !rider && !conductor) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el rider o el conductor asignado pueden cancelar");
		}
		viajeService.cancelar(id);
		notificarCambioEstado(id, "CANCELADO", viajeService.obtenerConductorId(id));
		simpMessagingTemplate.convertAndSend((String) "/topic/viajes/solicitados",
				(Object) Map.of("viajeId", id, "estado", "CANCELADO"));
		return ResponseEntity.ok(Map.of("mensaje", "Viaje cancelado correctamente"));
	}

	@PatchMapping("/{id}/rechazar")
	public ResponseEntity<?> rechazar(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
		currentUserService.requireRol("CONDUCTOR", "ADMIN");
		Conductor conductor = conductorDelUsuarioActual();
		if (body != null && body.get("conductorId") instanceof Number raw) {
			Long pedido = raw.longValue();
			if (!currentUserService.isAdmin() && !conductor.getId().equals(pedido)) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "conductorId no coincide con tu sesion");
			}
		}

		String estadoNotificado = viajeService.rechazar(id, conductor.getId());
		notificarCambioEstado(id, estadoNotificado, conductor.getId());
		simpMessagingTemplate.convertAndSend((String) "/topic/viajes/solicitados",
				(Object) Map.of(
						"viajeId", id,
						"estado", estadoNotificado,
						"conductorId", conductor.getId()));
		return ResponseEntity.ok(Map.of(
				"mensaje", "Viaje rechazado correctamente",
				"estado", estadoNotificado,
				"conductorId", conductor.getId()));
	}

	private Conductor conductorDelUsuarioActual() {
		Conductor conductor = conductorService.buscarPorUsuario(currentUserService.userId());
		if (conductor == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Completa tu perfil de conductor antes de aceptar viajes");
		}
		return conductor;
	}

	private void exigirConductorAsignado(Long viajeId) {
		if (currentUserService.isAdmin()) {
			return;
		}
		currentUserService.requireRol("CONDUCTOR");
		if (!viajeService.esConductorUsuario(viajeId, currentUserService.userId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este viaje no te fue asignado");
		}
	}
}
