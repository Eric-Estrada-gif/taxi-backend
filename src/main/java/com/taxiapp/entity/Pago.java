package com.taxiapp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@OneToOne
	@JoinColumn(name = "viaje_id", nullable = false, unique = true)
	private Viaje viaje;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Metodo metodo;
	
	@Column(nullable = false, precision = 8, scale = 2)
	private BigDecimal monto;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Estado estado = Estado.PENDIENTE;
	
	@Column(name = "fecha_pago", nullable = false, updatable = false)
	private LocalDateTime fechaPago;
	
	@PrePersist
	protected void onCreate() {
		this.fechaPago = LocalDateTime.now();
	}
	
	public enum Metodo {
	    EFECTIVO, TARJETA, BILLETERA_DIGITAL, VIP
	}
	
	public enum Estado {
		PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO
	}
}
