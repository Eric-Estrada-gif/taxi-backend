package com.taxiapp.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Tarifa;
import com.taxiapp.repository.TarifaRepository;

@Service
public class TarifaService {

	private final TarifaRepository tarifaRepository;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public TarifaService(TarifaRepository tarifaRepository, JdbcTemplate jdbcTemplate) {
		this.tarifaRepository = tarifaRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Long registrarTarifa(String nombre, BigDecimal tarifaBase, BigDecimal precioKm,
			BigDecimal precioMinuto, LocalDate vigenteDesde) {
		String sql = "{call sp_tarifa_insertar(?, ?, ?, ?, ?, ?)}";
		
		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setString(1, nombre);
                    cs.setBigDecimal(2, tarifaBase);
                    cs.setBigDecimal(3, precioKm);
                    cs.setBigDecimal(4, precioMinuto);
                    cs.setDate(5, Date.valueOf(vigenteDesde));
                    cs.registerOutParameter(6, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(6);
                });
	}
	
	@Transactional
	public Tarifa buscarPorId(Long id) {
		return tarifaRepository.buscarPorId(id);
	}
	
	@Transactional
	public List<Tarifa> listarActivas() {
		return tarifaRepository.listarActivas();
	}
	
	public void desactivar(Long id) {
		jdbcTemplate.update("CALL sp_tarifa_desactivar(?)", id);
	}
}
