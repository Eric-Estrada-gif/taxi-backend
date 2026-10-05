package com.taxiapp.service;

import java.time.LocalDate;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Usuario;

@Service
public class DemoAccountService {

	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;

	public DemoAccountService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public Usuario asegurarUsuarioConRol(String rolPedido) {
		String rol = rolPedido == null ? "RIDER" : rolPedido.trim().toUpperCase();
		Usuario existente = buscarPrimeroPorRol(rol);
		if (existente != null) {
			if ("CONDUCTOR".equals(rol)) {
				asegurarPerfilConductor(existente.getId());
			}
			return existente;
		}

		String email = switch (rol) {
			case "CONDUCTOR" -> "conductor.beta@taxiapp.com";
			case "ADMIN" -> "admin.beta@taxiapp.com";
			default -> "rider.beta@taxiapp.com";
		};

		Usuario porEmail = buscarPorEmail(email);
		if (porEmail == null) {
			insertarUsuario(
					rol.equals("CONDUCTOR") ? "Carlos" : rol.equals("ADMIN") ? "Admin" : "Rider",
					"Beta",
					email,
					"999111222",
					rol);
			porEmail = buscarPorEmail(email);
		}

		if (porEmail == null) {
			throw new IllegalStateException("No se pudo crear el usuario demo " + rol);
		}

		if ("CONDUCTOR".equals(rol)) {
			asegurarPerfilConductor(porEmail.getId());
		}
		return porEmail;
	}

	public Long conductorIdDeUsuario(Long usuarioId) {
		if (usuarioId == null) {
			return null;
		}
		try {
			return jdbcTemplate.queryForObject(
					"SELECT id FROM conductor WHERE usuario_id = ?",
					Long.class,
					usuarioId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	private Usuario buscarPrimeroPorRol(String rol) {
		try {
			return jdbcTemplate.query(
					"""
							SELECT id, nombre, apellido, email, rol, foto_perfil
							FROM usuario
							WHERE rol = ? AND activo = 1
							ORDER BY id ASC
							LIMIT 1
							""",
					rs -> rs.next() ? mapear(rs) : null,
					rol);
		} catch (Exception e) {
			return null;
		}
	}

	private Usuario buscarPorEmail(String email) {
		try {
			return jdbcTemplate.query(
					"""
							SELECT id, nombre, apellido, email, rol, foto_perfil
							FROM usuario
							WHERE email = ?
							LIMIT 1
							""",
					rs -> rs.next() ? mapear(rs) : null,
					email);
		} catch (Exception e) {
			return null;
		}
	}

	private Usuario mapear(java.sql.ResultSet rs) throws java.sql.SQLException {
		Usuario usuario = new Usuario();
		usuario.setId(rs.getLong("id"));
		usuario.setNombre(rs.getString("nombre"));
		usuario.setApellido(rs.getString("apellido"));
		usuario.setEmail(rs.getString("email"));
		usuario.setFotoPerfil(rs.getString("foto_perfil"));
		String rol = rs.getString("rol");
		if (rol != null) {
			usuario.setRol(Usuario.Rol.valueOf(rol));
		}
		return usuario;
	}

	private void insertarUsuario(String nombre, String apellido, String email, String telefono, String rol) {
		String hash = passwordEncoder.encode("beta123");
		try {
			jdbcTemplate.update(
					"""
							INSERT INTO usuario (nombre, apellido, email, password, telefono, rol, activo)
							VALUES (?, ?, ?, ?, ?, ?, 1)
							""",
					nombre, apellido, email, hash, telefono, rol);
		} catch (Exception primera) {
			jdbcTemplate.update(
					"""
							INSERT INTO usuario (nombre, apellido, email, password, telefono, rol, activo, google_id)
							VALUES (?, ?, ?, ?, ?, ?, 1, ?)
							""",
					nombre, apellido, email, hash, telefono, rol, "demo-" + rol.toLowerCase() + "-" + email);
		}
	}

	public void asegurarPerfilConductor(Long usuarioId) {
		Long conductorId = conductorIdDeUsuario(usuarioId);
		if (conductorId != null) {
			marcarDisponible(conductorId);
			return;
		}

		Long vehiculoId = buscarVehiculoId("BETA-001");
		if (vehiculoId == null) {
			jdbcTemplate.update(
					"""
							INSERT INTO vehiculo (placa, marca, modelo, anio, color, soat_vigente_hasta)
							VALUES ('BETA-001', 'Toyota', 'Yaris', 2022, 'Blanco', ?)
							""",
					java.sql.Date.valueOf(LocalDate.now().plusYears(1)));
			vehiculoId = buscarVehiculoId("BETA-001");
		}

		try {
			jdbcTemplate.update(
					"""
							INSERT INTO conductor (usuario_id, numero_licencia, fecha_vencimiento_licencia,
							                       estado, calificacion_promedio, vehiculo_id)
							VALUES (?, 'Q12345678', ?, 'DISPONIBLE', 4.90, ?)
							""",
					usuarioId,
					java.sql.Date.valueOf(LocalDate.now().plusYears(2)),
					vehiculoId);
		} catch (Exception e) {
			jdbcTemplate.update(
					"""
							INSERT INTO conductor (usuario_id, numero_licencia, fecha_vencimiento_licencia,
							                       estado, calificacion_promedio)
							VALUES (?, 'Q12345678', ?, 'DISPONIBLE', 4.90)
							""",
					usuarioId,
					java.sql.Date.valueOf(LocalDate.now().plusYears(2)));
		}

		conductorId = conductorIdDeUsuario(usuarioId);
		if (conductorId != null && vehiculoId != null) {
			jdbcTemplate.update("UPDATE vehiculo SET conductor_id = ? WHERE id = ? AND conductor_id IS NULL",
					conductorId, vehiculoId);
		}
	}

	private void marcarDisponible(Long conductorId) {
		jdbcTemplate.update("UPDATE conductor SET estado = 'DISPONIBLE' WHERE id = ?", conductorId);
	}

	private Long buscarVehiculoId(String placa) {
		try {
			return jdbcTemplate.queryForObject("SELECT id FROM vehiculo WHERE placa = ?", Long.class, placa);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}
}
