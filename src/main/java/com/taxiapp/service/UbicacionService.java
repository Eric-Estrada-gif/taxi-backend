package com.taxiapp.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Ubicacion;
import com.taxiapp.repository.UbicacionRepository;

@Service
public class UbicacionService {

	private final UbicacionRepository ubicacionRepository;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public UbicacionService(UbicacionRepository ubicacionRepository,
			JdbcTemplate jdbcTemplate) {
		this.ubicacionRepository = ubicacionRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Long registrarUbicacion(Long viajeId, BigDecimal lat, BigDecimal lng) {
		String sql = "{call sp_ubicacion_insertar(?, ?, ?, ?)}";
		
		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setLong(1, viajeId);
                    cs.setBigDecimal(2, lat);
                    cs.setBigDecimal(3, lng);
                    cs.registerOutParameter(4, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(4);
                });
    }
	
	@Transactional
	public List<Ubicacion> listarPorViaje(Long viajeId) {
		return ubicacionRepository.listarPorVaje(viajeId);
	}
	
	@Transactional
	public Ubicacion obtenerUltima(Long viajeId) {
		try {
			List<Ubicacion> filas = jdbcTemplate.query(
					"""
							SELECT id, lat, lng, timestamp
							FROM ubicacion
							WHERE viaje_id = ?
							ORDER BY timestamp DESC, id DESC
							LIMIT 1
							""",
					(rs, rowNum) -> {
						Ubicacion ubicacion = new Ubicacion();
						ubicacion.setId(rs.getLong("id"));
						ubicacion.setLat(rs.getBigDecimal("lat"));
						ubicacion.setLng(rs.getBigDecimal("lng"));
						if (rs.getTimestamp("timestamp") != null) {
							ubicacion.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
						}
						return ubicacion;
					},
					viajeId);
			return filas.isEmpty() ? null : filas.get(0);
		} catch (Exception e) {
			return null;
		}
	}
}
