package com.taxiapp.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.entity.MensajeViaje;
import com.taxiapp.repository.MensajeViajeRepository;
import com.taxiapp.security.AuthPrincipal;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final MensajeViajeRepository mensajeRepository;
	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;

	public ChatController(MensajeViajeRepository mensajeRepository, ViajeService viajeService,
			CurrentUserService currentUserService) {
		this.mensajeRepository = mensajeRepository;
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
	}

	@GetMapping("/historial/{viajeId}")
	public List<MensajeViaje> obtenerHistorial(@PathVariable Long viajeId) {
		viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
		return mensajeRepository.findByViajeIdOrderByFechaEnvioAsc(viajeId);
	}

	@MessageMapping("/chat.enviar/{viajeId}")
	@SendTo("/topic/viajes/{viajeId}/chat")
	public MensajeViaje enviarMensaje(@DestinationVariable Long viajeId, MensajeViaje mensaje, Principal principal) {
		if (!(principal instanceof AuthPrincipal auth)
				|| (!auth.tieneRol("ADMIN")
						&& !viajeService.esRider(viajeId, auth.getUserId())
						&& !viajeService.esConductorUsuario(viajeId, auth.getUserId()))) {
			throw new MessagingException("No autorizado para este chat");
		}

		mensaje.setViajeId(viajeId);
		mensaje.setFechaEnvio(LocalDateTime.now());
		if (auth.tieneRol("CONDUCTOR")) {
			mensaje.setRemitenteType("CONDUCTOR");
		} else {
			mensaje.setRemitenteType("RIDER");
		}
		return mensajeRepository.save(mensaje);
	}
}
