package com.taxiapp.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.VehiculoRegistradoDTO;
import com.taxiapp.entity.Vehiculo;
import com.taxiapp.service.VehiculoService;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {
	
	private final VehiculoService vehiculoService;
	
	@Autowired
	public VehiculoController(VehiculoService vehiculoService) {
		this.vehiculoService = vehiculoService;
	}
	
	//POST http://localhost:8080/api/vehiculos
	@PostMapping
	public ResponseEntity<?> registrar(@RequestBody VehiculoRegistradoDTO dto) {
		Long id = vehiculoService.registrarVehiculo(
				dto.getPlaca(),
				dto.getMarca(),
				dto.getModelo(),
				dto.getAnio(),
				dto.getColor(),
				dto.getSoatVigenteHasta()
				);
		
		return ResponseEntity.ok(Map.of("id", id, "mensaje", "Vehiculo registrado correctamente"));
	}
	
	//GET http://localhost:8080/api/vehiculos/1
	@GetMapping("/{id}")
	public ResponseEntity<Vehiculo> buscarPorId(@PathVariable Long id) {
		Vehiculo vehiculo = vehiculoService.buscarPorId(id);
		if(vehiculo == null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(vehiculo);
	}
	
	//GET http://localhost:8080/api/vehiculos/placa/ABC-123
	@GetMapping("/placa/{placa}")
	public ResponseEntity<Vehiculo> buscarPorPlaca(@PathVariable String placa) {
		Vehiculo vehiculo = vehiculoService.buscarPorPlaca(placa);
		
		if(vehiculo == null) {
			return ResponseEntity.notFound().build();
		}
		
		return ResponseEntity.ok(vehiculo);
	}
}
