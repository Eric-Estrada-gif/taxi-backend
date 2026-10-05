package com.taxiapp.entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehiculo {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@JsonIgnore
	@OneToOne(mappedBy = "vehiculo")
	private	Conductor conductor;
	
	@Column(nullable = false, unique = true, length = 10)
	private String placa;
	
	@Column(nullable = false, length = 50)
	private String marca;
	
	@Column(nullable = false, length = 50)
	private String modelo;
	
	@Column(name = "anio")
	private Integer anio;
	
	@Column(length = 30)
	private String color;
	
	@Column(name = "soat_vigente_hasta")
	private LocalDate soaVigenteHasta;
}
