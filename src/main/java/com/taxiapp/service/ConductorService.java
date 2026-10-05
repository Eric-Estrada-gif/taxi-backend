package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Conductor;
import com.taxiapp.entity.Usuario;
import com.taxiapp.entity.Vehiculo;
import com.taxiapp.repository.ConductorRepository;

@Service
public class ConductorService {

	private final ConductorRepository conductorRepository;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public ConductorService(ConductorRepository conductorRepository, JdbcTemplate jdbcTemplate) {
		this.conductorRepository = conductorRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Long registrarConductor(Long usuarioId, String numeroLicencia, LocalDate fechaVencimientoLicencia,
			Long vehiculoId) {
		
		String sql = "{call sp_conductor_insertar(?, ?, ?, ?, ?)}";
		 
        return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setLong(1, usuarioId);
                    cs.setString(2, numeroLicencia);
                    cs.setDate(3, fechaVencimientoLicencia != null ? Date.valueOf(fechaVencimientoLicencia) : null);
                    cs.setLong(4, vehiculoId);
                    cs.registerOutParameter(5, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(5);
                });
	}
	
	@Transactional
	public Conductor buscarPorId(Long id) {
		return conductorRepository.buscarPorId(id);
	}
	
	@Transactional
	public Conductor buscarPorUsuario(Long usuarioId) {
		try {
			Conductor deSp = conductorRepository.buscarPorUsuario(usuarioId);
			if (deSp != null) {
				return deSp;
			}
		} catch (Exception ignored) {
		}
		return buscarPorUsuarioJdbc(usuarioId);
	}

	public Conductor buscarPorUsuarioJdbc(Long usuarioId) {
		List<Conductor> filas = jdbcTemplate.query(
				"""
						SELECT id, usuario_id, vehiculo_id, numero_licencia, estado, calificacion_promedio
						FROM conductor
						WHERE usuario_id = ?
						LIMIT 1
						""",
				(rs, rowNum) -> {
					Conductor conductor = new Conductor();
					conductor.setId(rs.getLong("id"));
					Usuario usuario = new Usuario();
					usuario.setId(rs.getLong("usuario_id"));
					conductor.setUsuario(usuario);
					long vehiculoId = rs.getLong("vehiculo_id");
					if (!rs.wasNull()) {
						Vehiculo vehiculo = new Vehiculo();
						vehiculo.setId(vehiculoId);
						conductor.setVehiculo(vehiculo);
					}
					conductor.setNumeroLicencia(rs.getString("numero_licencia"));
					String estado = rs.getString("estado");
					if (estado != null) {
						conductor.setEstado(Conductor.Estado.valueOf(estado));
					}
					conductor.setCalificacionPromedio(rs.getBigDecimal("calificacion_promedio"));
					return conductor;
				},
				usuarioId);
		return filas.isEmpty() ? null : filas.get(0);
	}
	
	@Transactional
	public List<Conductor> listarPorEstado(String estado) {
		return conductorRepository.listarPorEstado(estado);
	}
	
	public void actualizarEstado(Long id, String estado) {
		jdbcTemplate.update("CALL sp_conductor_actualizar_estado(?, ?)", id, estado);
	}
}
