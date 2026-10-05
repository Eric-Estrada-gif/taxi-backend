package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Vehiculo;
import com.taxiapp.repository.VehiculoRepository;

@Service
public class VehiculoService {
	
	private final VehiculoRepository vehiculoRepository;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public VehiculoService(VehiculoRepository vehiculoRepository, JdbcTemplate jdbcTemplate) {
		this.vehiculoRepository = vehiculoRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Long registrarVehiculo(String placa, String marca, String modelo,
			Integer anio, String color, LocalDate soatVigenteHasta) {
		
		String sql = "{call sp_vehiculo_insertar(?, ?, ?, ?, ?, ?, ?)}";
		
		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setString(1, placa);
                    cs.setString(2, marca);
                    cs.setString(3, modelo);
                    cs.setInt(4, anio);
                    cs.setString(5, color);
                    cs.setDate(6, soatVigenteHasta != null ? Date.valueOf(soatVigenteHasta) : null);
                    cs.registerOutParameter(7, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(7);
                });
	}
	
	@Transactional
	public Vehiculo buscarPorId(Long id) {
		return vehiculoRepository.buscarPorId(id);
	}
	
	@Transactional
	public Vehiculo buscarPorPlaca(String placa) {
		return vehiculoRepository.buscarPorPlaca(placa);
	}
}
