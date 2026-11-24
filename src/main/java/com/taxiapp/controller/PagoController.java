package com.taxiapp.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.PagoRegistroDTO;
import com.taxiapp.entity.Pago;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.CulqiService;
import com.taxiapp.service.PagoService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

	private final PagoService pagoService;
	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;
	private final CulqiService culqiService;
	
	@Autowired
	public PagoController(PagoService pagoService, ViajeService viajeService,
			CurrentUserService currentUserService, CulqiService culqiService) {
		this.pagoService = pagoService;
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
		this.culqiService = culqiService;
	}

	@PostMapping("/culqi/probar")
	public ResponseEntity<?> probarCulqi(@RequestBody Map<String, Object> body) {
		String token = texto(body.get("token"));
		String email = texto(body.get("email"));
		String deviceId = texto(body.get("deviceId"));
		String nombre = texto(body.get("nombre"));
		String telefono = texto(body.get("telefono"));
		Map<String, String> tresDs = mapa(body.get("autenticacion"));
		if (token.isBlank() || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Faltan el token de la tarjeta y el correo"));
		}
		try {
			Map<String, Object> resultado = culqiService.cobrarPrueba(token, email, deviceId, nombre, telefono, tresDs);
			return ResponseEntity.ok(resultado);
		} catch (IllegalStateException e) {
			return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(502).body(Map.of("error", "No se pudo hablar con Culqi"));
		}
	}
	
	@PostMapping("/culqi/viaje")
	public ResponseEntity<?> cobrarViaje(@RequestBody Map<String, Object> body) {
		Long viajeId = numero(body.get("viajeId"));
		String token = texto(body.get("token"));
		String email = texto(body.get("email"));
		if (viajeId == null || token.isBlank() || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Faltan el viaje, el token de la tarjeta o el correo"));
		}
		Long usuarioId = currentUserService.userId();
		if (!currentUserService.isAdmin() && !viajeService.esRider(viajeId, usuarioId)) {
			return ResponseEntity.status(403).body(Map.of("error", "Solo el pasajero puede pagar este viaje"));
		}
		String estadoViaje = viajeService.obtenerEstado(viajeId);
		if (!"FINALIZADO".equals(estadoViaje)) {
			return ResponseEntity.status(409).body(Map.of("error", "El viaje todavía no termina"));
		}
		String estadoPago = pagoService.estadoPorViaje(viajeId);
		if ("COMPLETADO".equals(estadoPago)) {
			return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Este viaje ya fue cobrado"));
		}
		BigDecimal monto = viajeService.obtenerMonto(viajeId);
		if (monto == null || monto.signum() <= 0) {
			return ResponseEntity.badRequest().body(Map.of("error", "El viaje no tiene un monto para cobrar"));
		}
		int centimos = monto.setScale(2, RoundingMode.HALF_UP).movePointRight(2).setScale(0, RoundingMode.HALF_UP)
				.intValueExact();
		try {
			Map<String, Object> resultado = culqiService.cobrar(
					token,
					email,
					texto(body.get("deviceId")),
					texto(body.get("nombre")),
					texto(body.get("apellido")),
					texto(body.get("telefono")),
					mapa(body.get("autenticacion")),
					centimos,
					"Viaje RapiTrip " + viajeId);
			boolean ok = Boolean.TRUE.equals(resultado.get("ok"));
			boolean pideAuth = Boolean.TRUE.equals(resultado.get("requiereAutenticacion"));
			if (ok) {
				pagoService.dejarEstado(viajeId, "TARJETA", monto, "COMPLETADO");
			} else if (!pideAuth) {
				pagoService.dejarEstado(viajeId, "TARJETA", monto, "FALLIDO");
			}
			resultado.put("monto", monto);
			return ResponseEntity.ok(resultado);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		} catch (IllegalStateException e) {
			return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(502).body(Map.of("error", "No se pudo hablar con Culqi"));
		}
	}

	// POST http://localhost:8080/api/pagos
    // body: { "viajeId": 1, "metodo": "EFECTIVO", "monto": 21.44 }
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody PagoRegistroDTO dto) {
        viajeService.exigirParticipante(dto.getViajeId(), currentUserService.userId(), currentUserService.isAdmin());
        Long id = pagoService.registrarPago(dto.getViajeId(), dto.getMetodo(), dto.getMonto());
        return ResponseEntity.ok(Map.of("id", id, "mensaje", "Pago registrado correctamente"));
    }
 
    // GET http://localhost:8080/api/pagos/1
    @GetMapping("/{id}")
    public ResponseEntity<Pago> buscarPorId(@PathVariable Long id) {
        Pago pago = pagoService.buscarPorId(id);
        if (pago == null) {
            return ResponseEntity.notFound().build();
        }
        if (pago.getViaje() != null) {
            viajeService.exigirParticipante(pago.getViaje().getId(), currentUserService.userId(),
                    currentUserService.isAdmin());
        }
        return ResponseEntity.ok(pago);
    }
 
    // GET http://localhost:8080/api/pagos/viaje/1
    @GetMapping("/viaje/{viajeId}")
    public ResponseEntity<Pago> buscarPorViaje(@PathVariable Long viajeId) {
        viajeService.exigirParticipante(viajeId, currentUserService.userId(), currentUserService.isAdmin());
        Pago pago = pagoService.buscarPorViaje(viajeId);
        if (pago == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pago);
    }
 
    // PATCH http://localhost:8080/api/pagos/1/estado
    // body: { "estado": "COMPLETADO" }
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        currentUserService.requireRol("ADMIN");
        pagoService.actualizarEstado(id, body.get("estado"));
        return ResponseEntity.ok(Map.of("mensaje", "Estado actualizado correctamente"));
    }

	private static String texto(Object valor) {
		return valor == null ? "" : valor.toString().trim();
	}

	private static Long numero(Object valor) {
		if (valor instanceof Number n) {
			return n.longValue();
		}
		if (valor == null) {
			return null;
		}
		try {
			return Long.parseLong(valor.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, String> mapa(Object valor) {
		if (!(valor instanceof Map<?, ?> origen)) {
			return Map.of();
		}
		Map<String, String> out = new java.util.LinkedHashMap<>();
		origen.forEach((k, v) -> {
			if (k != null && v != null) {
				out.put(k.toString(), v.toString());
			}
		});
		return out;
	}
}
