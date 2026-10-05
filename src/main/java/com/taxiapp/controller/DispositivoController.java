package com.taxiapp.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.FcmService;

@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

	private final FcmService fcmService;
	private final CurrentUserService currentUserService;

	public DispositivoController(FcmService fcmService, CurrentUserService currentUserService) {
		this.fcmService = fcmService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/fcm-token")
	public ResponseEntity<?> registrarToken(@RequestBody Map<String, String> body) {
		String token = body.get("token");
		fcmService.guardarToken(currentUserService.userId(), token);
		return ResponseEntity.ok(Map.of("mensaje", "Token FCM registrado"));
	}
}
