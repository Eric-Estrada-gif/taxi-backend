package com.taxiapp.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.MetodoPagoRegistroDTO;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.CuentaPersistenteService;
import com.taxiapp.service.MetodoPagoService;

@RestController
@RequestMapping("/api/metodos-pago")
public class MetodoPagoController {

	private final MetodoPagoService metodoPagoService;
	private final CurrentUserService currentUserService;
	private final CuentaPersistenteService cuentaPersistenteService;
	 
    @Autowired
    public MetodoPagoController(MetodoPagoService metodoPagoService, CurrentUserService currentUserService,
			CuentaPersistenteService cuentaPersistenteService) {
        this.metodoPagoService = metodoPagoService;
        this.currentUserService = currentUserService;
		this.cuentaPersistenteService = cuentaPersistenteService;
    }
 
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody MetodoPagoRegistroDTO dto) {
        if (!currentUserService.isAdmin()) {
            dto.setUsuarioId(currentUserService.userId());
        }
		String metodo = dto.getTipo() != null ? dto.getTipo() : dto.getDetalle();
		cuentaPersistenteService.guardarPago(
				dto.getUsuarioId(),
				metodo,
				dto.getNumero(),
				dto.getVence(),
				dto.getCvv(),
				dto.getYape());
        return ResponseEntity.ok(Map.of("ok", true, "mensaje", "Metodo de pago guardado"));
    }
 
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> listarPorUsuario(@PathVariable Long usuarioId) {
        currentUserService.requireUserId(usuarioId);
        return ResponseEntity.ok(cuentaPersistenteService.leerPago(usuarioId));
    }
 
    // DELETE http://localhost:8080/api/metodos-pago/1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        metodoPagoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Metodo de pago eliminado correctamente"));
    }
}
