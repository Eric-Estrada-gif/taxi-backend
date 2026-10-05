package com.taxiapp.controller;

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
import com.taxiapp.service.PagoService;
import com.taxiapp.service.ViajeService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

	private final PagoService pagoService;
	private final ViajeService viajeService;
	private final CurrentUserService currentUserService;
	
	@Autowired
	public PagoController(PagoService pagoService, ViajeService viajeService,
			CurrentUserService currentUserService) {
		this.pagoService = pagoService;
		this.viajeService = viajeService;
		this.currentUserService = currentUserService;
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
}
