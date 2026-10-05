package com.taxiapp.controller;

import java.util.List;
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
import com.taxiapp.entity.MetodoPago;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.MetodoPagoService;

@RestController
@RequestMapping("/api/metodos-pago")
public class MetodoPagoController {

	private final MetodoPagoService metodoPagoService;
	private final CurrentUserService currentUserService;
	 
    @Autowired
    public MetodoPagoController(MetodoPagoService metodoPagoService, CurrentUserService currentUserService) {
        this.metodoPagoService = metodoPagoService;
        this.currentUserService = currentUserService;
    }
 
    // POST http://localhost:8080/api/metodos-pago
    // body: { "usuarioId": 1, "tipo": "TARJETA", "detalle": "Visa **** 4321", "predeterminado": true }
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody MetodoPagoRegistroDTO dto) {
        if (!currentUserService.isAdmin()) {
            dto.setUsuarioId(currentUserService.userId());
        }
        Long id = metodoPagoService.registrarMetodoPago(
                dto.getUsuarioId(), dto.getTipo(), dto.getDetalle(), dto.getPredeterminado());
        return ResponseEntity.ok(Map.of("id", id, "mensaje", "Metodo de pago registrado correctamente"));
    }
 
    // GET http://localhost:8080/api/metodos-pago/usuario/1
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<MetodoPago>> listarPorUsuario(@PathVariable Long usuarioId) {
        currentUserService.requireUserId(usuarioId);
        return ResponseEntity.ok(metodoPagoService.listarPorUsuario(usuarioId));
    }
 
    // DELETE http://localhost:8080/api/metodos-pago/1
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        metodoPagoService.eliminar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Metodo de pago eliminado correctamente"));
    }
}
