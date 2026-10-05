package com.taxiapp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taxiapp.dto.EstimacionRequestDTO;
import com.taxiapp.dto.EstimacionResponseDTO;
import com.taxiapp.entity.Tarifa;

@Service
public class EstimacionService {
 
    private final GoogleMapsService googleMapsService;
    private final TarifaService tarifaService;
 
    @Autowired
    public EstimacionService(GoogleMapsService googleMapsService, TarifaService tarifaService) {
        this.googleMapsService = googleMapsService;
        this.tarifaService = tarifaService;
    }
 
    public List<EstimacionResponseDTO> estimar(EstimacionRequestDTO request) {
 
        GoogleMapsService.DistanciaResultado distancia = googleMapsService.calcularDistancia(
                request.getOrigenLat(), request.getOrigenLng(),
                request.getDestinoLat(), request.getDestinoLng()
        );
 
        List<Tarifa> tarifasActivas = tarifaService.listarActivas();
 
        return tarifasActivas.stream()
                .map(tarifa -> {
                    // precio = tarifa_base + (precio_km * distancia_km) + (precio_minuto * duracion_min)
                    BigDecimal precioPorDistancia = tarifa.getPrecioKm()
                            .multiply(BigDecimal.valueOf(distancia.distanciaKm()));
                    BigDecimal precioPorTiempo = tarifa.getPrecioMinuto()
                            .multiply(BigDecimal.valueOf(distancia.duracionMinutos()));
 
                    BigDecimal precioEstimado = tarifa.getTarifaBase()
                            .add(precioPorDistancia)
                            .add(precioPorTiempo)
                            .setScale(2, RoundingMode.HALF_UP);
 
                    return new EstimacionResponseDTO(
                            tarifa.getId(),
                            tarifa.getNombre(),
                            precioEstimado,
                            distancia.distanciaKm(),
                            distancia.duracionMinutos()
                    );
                })
                .collect(Collectors.toList());
    }
}
