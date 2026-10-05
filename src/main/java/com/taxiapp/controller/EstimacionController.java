package com.taxiapp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.taxiapp.dto.EstimacionRequestDTO;
import com.taxiapp.dto.EstimacionResponseDTO;
import com.taxiapp.service.EstimacionService;

@RestController
@RequestMapping("/api/viajes")
public class EstimacionController {
 
    private final EstimacionService estimacionService;
 
    @Autowired
    public EstimacionController(EstimacionService estimacionService) {
        this.estimacionService = estimacionService;
    }
 
    // POST http://localhost:8080/api/viajes/estimar
    @PostMapping("/estimar")
    public ResponseEntity<List<EstimacionResponseDTO>> estimar(@RequestBody EstimacionRequestDTO request) {
        return ResponseEntity.ok(estimacionService.estimar(request));
    }
}
