package com.taxiapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.service.RegistroPadronService;
import com.taxiapp.service.TestConductaService;

@RestController
@RequestMapping("/api/registro")
public class RegistroController {

	private final RegistroPadronService padron;
	private final TestConductaService test;

	public RegistroController(RegistroPadronService padron, TestConductaService test) {
		this.padron = padron;
		this.test = test;
	}

	@GetMapping("/padron/ejemplos")
	public ResponseEntity<?> ejemplos() {
		return ResponseEntity.ok(padron.resumenPadron());
	}

	@GetMapping("/padron/dni")
	public ResponseEntity<?> dni(@RequestParam String dni) {
		Map<String, Object> fila = padron.dni(dni);
		return fila == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(fila);
	}

	@GetMapping("/padron/licencia")
	public ResponseEntity<?> licencia(@RequestParam String dni) {
		Map<String, Object> fila = padron.licencia(dni);
		return fila == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(fila);
	}

	@GetMapping("/padron/soat")
	public ResponseEntity<?> soat(@RequestParam String placa) {
		Map<String, Object> fila = padron.soat(placa);
		return fila == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(fila);
	}

	@PostMapping("/verificar")
	public ResponseEntity<?> verificar(@RequestBody Map<String, Object> body) {
		Long conductorId = asLong(body.get("conductorId"));
		String dni = texto(body.get("dni"));
		String nombres = texto(body.get("nombres"));
		String licencia = texto(body.get("numeroLicencia"));
		String placa = texto(body.get("placa"));
		if (dni.isBlank() || placa.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Faltan dni y placa"));
		}
		return ResponseEntity.ok(padron.verificar(conductorId, dni, nombres, licencia, placa));
	}

	@GetMapping("/test/preguntas")
	public ResponseEntity<?> preguntas(@RequestParam String rol) {
		try {
			return ResponseEntity.ok(Map.of(
					"rol", rol.toUpperCase(),
					"cantidad", TestConductaService.PREGUNTAS_POR_INTENTO,
					"notaMinima", TestConductaService.NOTA_MINIMA,
					"preguntas", test.preguntasAleatorias(rol)));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/test/estado")
	public ResponseEntity<?> estadoTest(@RequestParam String email,
			@RequestParam(required = false) String telefono,
			@RequestParam String rol) {
		try {
			return ResponseEntity.ok(test.estado(email, telefono, rol));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/test/enviar")
	public ResponseEntity<?> enviarTest(@RequestBody Map<String, Object> body) {
		try {
			String email = texto(body.get("email"));
			String telefono = texto(body.get("telefono"));
			String rol = texto(body.get("rol"));
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> respuestas = (List<Map<String, Object>>) body.get("respuestas");
			return ResponseEntity.ok(test.calificar(email, telefono, rol, respuestas));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	private static String texto(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static Long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null || v.toString().isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
