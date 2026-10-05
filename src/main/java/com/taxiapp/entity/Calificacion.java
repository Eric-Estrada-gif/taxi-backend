package com.taxiapp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "calificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Calificacion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "viaje_id", nullable = false)
	private Viaje viaje;
	
	@ManyToOne
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;
	
	@Column(nullable = false)
	private Byte puntaje;
	
	@Column(length = 500)
	private String comentario;
	
	@Column(nullable = false, updatable = false)
	private LocalDateTime fecha;
	
	@PrePersist
	protected void onCreate() {
		this.fecha = LocalDateTime.now();
	}
}
