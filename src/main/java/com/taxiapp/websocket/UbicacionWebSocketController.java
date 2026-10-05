package com.taxiapp.websocket;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.taxiapp.security.AuthPrincipal;
import com.taxiapp.service.UbicacionService;
import com.taxiapp.service.ViajeService;

import lombok.Getter;
import lombok.Setter;

@Controller
public class UbicacionWebSocketController {

	private final SimpMessagingTemplate simpMessagingTemplate;
	private final UbicacionService ubicacionService;
	private final ViajeService viajeService;

	public UbicacionWebSocketController(SimpMessagingTemplate simpMessagingTemplate,
			UbicacionService ubicacionService, ViajeService viajeService) {
		this.simpMessagingTemplate = simpMessagingTemplate;
		this.ubicacionService = ubicacionService;
		this.viajeService = viajeService;
	}

	@MessageMapping("/viajes/{viajeId}/ubicacion")
	public void recibirUbicacion(@DestinationVariable Long viajeId, UbicacionMensaje mensaje, Principal principal) {
		if (!(principal instanceof AuthPrincipal auth)
				|| (!auth.tieneRol("ADMIN") && !viajeService.esConductorUsuario(viajeId, auth.getUserId()))) {
			return;
		}

		ubicacionService.registrarUbicacion(viajeId, mensaje.getLat(), mensaje.getLng());

		UbicacionMensaje salida = new UbicacionMensaje();
		salida.setLat(mensaje.getLat());
		salida.setLng(mensaje.getLng());
		salida.setTimestamp(LocalDateTime.now().toString());

		simpMessagingTemplate.convertAndSend((String) ("/topic/viajes/" + viajeId + "/ubicacion"), (Object) salida);
	}

	@Getter
	@Setter
	public static class UbicacionMensaje {
		private BigDecimal lat;
		private BigDecimal lng;
		private String timestamp;
	}
}
