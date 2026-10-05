package com.taxiapp.entity;

import java.math.BigDecimal;
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
@Table(name = "ubicacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "viaje_id", nullable = false)
	private Viaje viaje;
	
	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lat;
	
	@Column(nullable = false, precision = 10, scale = 7)
	private BigDecimal lng;
	
	@Column(name = "timestamp", nullable = false, updatable = false)
	private LocalDateTime timestamp;
	
	@PrePersist
	protected void onCreate() {
		this.timestamp = LocalDateTime.now();
	}
}
