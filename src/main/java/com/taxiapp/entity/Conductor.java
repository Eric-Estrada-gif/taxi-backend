package com.taxiapp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conductor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Conductor {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;
	
	@Column(name = "numero_licencia", nullable = false, length = 50)
	private String numeroLicencia;
	
	@Column(name = "fecha_vencimiento_licencia")
	private LocalDate fechaVencimientoLicencia;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Estado estado = Estado.OFFLINE;
	
	@Column(name = "calificacion_promedio", precision = 3, scale = 2)
	private BigDecimal calificacionPromedio = BigDecimal.ZERO;
	
	@OneToOne
	@JoinColumn(name = "vehiculo_id", unique = true)
	private Vehiculo vehiculo;
	
	public enum Estado{
		DISPONIBLE, OCUPADO, OFFLINE;
	}
	

}
