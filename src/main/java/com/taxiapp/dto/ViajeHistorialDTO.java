package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ViajeHistorialDTO {

	private Long id;
	private String fecha;
    private String horaInicio;
    private String horaFin;
    private String origen;
    private String destino;
    private String categoria;
    private String estado;
    private Double costoTotal;
    
 // Detalles del conductor, vehículo y usuario
    private String conductorNombre;
    private String conductorDetalleAuto; // Ej: "Gris Suzuki Swift, APX011"
    private int calificacionEstrellas;
    private String metodoPago;   // EFECTIVO, TARJETA, BILLETERA_DIGITAL, VIP

    /** Usuario rider del viaje. El conductor lo necesita en el detalle. */
    private Long riderId;
    private String riderNombre;
    private Double origenLat;
    private Double origenLng;
    private Double destinoLat;
    private Double destinoLng;
}
