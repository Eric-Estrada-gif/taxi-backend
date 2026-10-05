package com.taxiapp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tarifa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tarifa {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 50)
	private String nombre; //Economica, Confort, Priorirdad
	
	@Column(name = "tarifa_base", nullable = false, precision = 6, scale = 2)
	private BigDecimal tarifaBase;
	
	@Column(name = "precio_km", nullable = false, precision = 6, scale = 2)
	private BigDecimal precioKm;
	
	@Column(name = "precio_minuto", nullable = false, precision = 6, scale = 2)
	private BigDecimal precioMinuto = BigDecimal.ZERO;
	
	@Column(name = "vigente_desde", nullable = false)
	private LocalDate vigenteDesde;
	
	@Column(nullable = false)
	private Boolean activo = true;
}
