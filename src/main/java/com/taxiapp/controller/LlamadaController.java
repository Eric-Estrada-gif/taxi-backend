package com.taxiapp.controller;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.security.AuthPrincipal;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.ViajeService;

import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;

@RestController
@RequestMapping("/api/llamadas")
public class LlamadaController {

	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;

	@Value("${agora.app-id:}")
	private String appId;

	@Value("${agora.app-certificate:}")
	private String certificado;

	public LlamadaController(ViajeService viajeService, CurrentUserService currentUserService) {
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/token")
	public ResponseEntity<?> token(@RequestBody Map<String, Object> body) {
		if (appId.isBlank() || certificado.isBlank()) {
			return ResponseEntity.status(503).body(Map.of("error", "Falta la configuración de Agora en el servidor"));
		}
		Object crudo = body.get("viajeId");
		if (!(crudo instanceof Number numero)) {
			return ResponseEntity.badRequest().body(Map.of("error", "Falta el viaje"));
		}
		long viajeId = numero.longValue();
		viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
		int uid = uidDe(currentUserService.userId());
		String canal = "viaje" + viajeId;
		String token = new RtcTokenBuilder2().buildTokenWithUid(
				appId, certificado, canal, uid, Role.ROLE_PUBLISHER, 3600, 3600);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("appId", appId);
		out.put("canal", canal);
		out.put("token", token);
		out.put("uid", uid);
		return ResponseEntity.ok(out);
	}

	@MessageMapping("/llamada.senal/{viajeId}")
	@SendTo("/topic/viajes/{viajeId}/llamada")
	public Map<String, String> senal(@DestinationVariable Long viajeId, Map<String, Object> body, Principal principal) {
		if (!(principal instanceof AuthPrincipal auth)
				|| (!auth.tieneRol("ADMIN")
						&& !viajeService.esRider(viajeId, auth.getUserId())
						&& !viajeService.esConductorUsuario(viajeId, auth.getUserId()))) {
			throw new MessagingException("No autorizado para esta llamada");
		}
		String accion = body.get("accion") == null ? "" : body.get("accion").toString();
		String inicio = body.get("inicio") == null ? "" : body.get("inicio").toString();
		String de = auth.tieneRol("CONDUCTOR") ? "CONDUCTOR" : "RIDER";
		Map<String, String> out = new LinkedHashMap<>();
		out.put("tipo", "llamada");
		out.put("accion", accion);
		out.put("de", de);
		if (!inicio.isBlank()) {
			out.put("inicio", inicio);
		}
		return out;
	}

	private static int uidDe(Long usuarioId) {
		if (usuarioId == null || usuarioId <= 0) {
			return 1;
		}
		long valor = usuarioId % 2147483647L;
		return valor == 0 ? 1 : (int) valor;
	}
}
