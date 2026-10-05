package com.taxiapp.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, length = 100)
	private String nombre;
	
	@Column(nullable = false, length = 100)
	private String apellido;
	
	@Column(nullable = false, unique = true, length = 150)
	private String email;
	
	@Column(name = "google_id", unique = true, length = 255)
	private String googleId;
	
	// Nunca se expone por la API, pero Hibernate si lo necesita, internamente para poblar desde los SP.
	// Ahora es opcional porque el login es solo por google
	@JsonIgnore 
	@Column(nullable = false, length = 255)
	private String password;
	
	@Column(length = 20) //Cambiar luego
	private String telefono;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Rol rol;
	
	@Column(name = "foto_perfil", length = 512)
	private String fotoPerfil;
	
	@Column(name = "fecha_registro", nullable = false, updatable = false)
	private LocalDateTime fechaRegistro;
	
	@Column(nullable = false)
	private Boolean activo = true;
	
	@PrePersist
	protected void onCreate() {
		this.fechaRegistro = LocalDateTime.now();
	}
	
	public enum Rol{
		RIDER, CONDUCTOR, ADMIN
	}
	
}
