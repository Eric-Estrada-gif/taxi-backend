package com.taxiapp.controller;

import java.util.List;
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

import com.taxiapp.dto.TarifaRegistroDTO;
import com.taxiapp.entity.Tarifa;
import com.taxiapp.security.CurrentUserService;
import com.taxiapp.service.TarifaService;

@RestController
@RequestMapping("/api/tarifas")
public class TarifaController {

	private final TarifaService tarifaService;
	private final CurrentUserService currentUserService;
	
	@Autowired
	public TarifaController(TarifaService tarifaService, CurrentUserService currentUserService) {
		this.tarifaService = tarifaService;
		this.currentUserService = currentUserService;
	}
	
	// POST http://localhost:8080/api/tarifas
	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody TarifaRegistroDTO dto) {
		currentUserService.requireRol("ADMIN");
		Long id = tarifaService.registrarTarifa(
				dto.getNombre(),
				dto.getTarifaBase(),
				dto.getPrecioKm(),
				dto.getPrecioMinuto(),
				dto.getVigenteDesde()
				);
		
		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Tarifa registrada correctamente"));
	}
	
	// GET http://localhost:8080/api/tarifas/1
	@GetMapping("/{id}")
	public ResponseEntity<Tarifa> buscarPorId(@PathVariable Long id) {
		Tarifa tarifa = tarifaService.buscarPorId(id);
		if(tarifa == null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(tarifa);
	}
	
	// GET http://localhost:8080/api/tarifas/activas
	@GetMapping("/activas")
	public ResponseEntity<List<Tarifa>> listarActivas() {
		return ResponseEntity.ok(tarifaService.listarActivas());
	}
	
	// PATCH http://localhost:8080/api/tarifas/1/desactivar
	@PatchMapping("/{id}/desactivar")
	public ResponseEntity<?> desactivar(@PathVariable Long id) {
		currentUserService.requireRol("ADMIN");
		tarifaService.desactivar(id);
		return ResponseEntity.ok(Map.of("mensaje", "Tarifa desactivada correctamente"));
	}
}
