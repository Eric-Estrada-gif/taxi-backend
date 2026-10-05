package com.taxiapp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "viaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Viaje {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "rider_id", nullable = false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "viajes", "password"})
	private Usuario rider;
	
	@ManyToOne
	@JoinColumn(name = "conductor_id")
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "viajes", "vehiculo"})
	private Conductor conductor;
	
	@ManyToOne
	@JoinColumn(name = "tarifa_id", nullable = false)
	@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
	private Tarifa tarifa;
	
	@Column(name = "origen_direccion", nullable = false, length = 255)
	private String origenDireccion;
	
	@Column(name = "origen_lat", nullable = false, precision = 7)
	private BigDecimal origenLat;
	
	@Column(name = "origen_lng", nullable = false, precision = 7)
	private BigDecimal origenLng;
	
	@Column(name = "destino_direccion", nullable = false, length = 255)
	private String destinoDireccion;
	
	@Column(name = "destino_lat", nullable = false, precision = 10, scale = 7)
	private BigDecimal destinoLat;
	
	@Column(name = "destino_lng", nullable = false, precision = 10, scale = 7)
	private BigDecimal destinoLng;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Estado estado = Estado.SOLICITADO;
	
	@Column(name = "monto_total", precision = 8, scale = 2)
	private BigDecimal montoTotal;
	
	@Column(name = "fecha_solicitud", nullable = false, updatable = false)
	private LocalDateTime fechaSolicitud;
	
	@Column(name = "fecha_inicio")
	private LocalDateTime fechaInicio;
	
	@Column(name = "fecha_fin")
	private LocalDateTime fechaFin;
	
	@PrePersist
	protected void onCreate() {
		this.fechaSolicitud = LocalDateTime.now();
	}
	
	public enum Estado{
		SOLICITADO, ACEPTADO, EN_CURSO, FINALIZADO, CANCELADO
	}
}
