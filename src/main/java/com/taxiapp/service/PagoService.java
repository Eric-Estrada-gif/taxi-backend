package com.taxiapp.service;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Types;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Pago;
import com.taxiapp.repository.PagoRepository;

@Service
public class PagoService {

	private final PagoRepository pagoRepository;
	private final JdbcTemplate jdbcTemplate;
	
	@Autowired
	public PagoService(PagoRepository pagoRepository, JdbcTemplate jdbcTemplate) {
		this.pagoRepository = pagoRepository;
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Long registrarPago(Long viajeId, String metodo, BigDecimal monto) {
		String sql = "{call sp_pago_insertar(?, ?, ?, ?)}";
		
		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setLong(1, viajeId);
                    cs.setString(2, metodo);
                    cs.setBigDecimal(3, monto);
                    cs.registerOutParameter(4, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(4);
                });
    }
	
	@Transactional
	public Pago buscarPorId(Long id) {
		return pagoRepository.buscarPorId(id);
	}
	
	@Transactional
	public Pago buscarPorViaje(Long viajeId) {
		return pagoRepository.buscarPorViaje(viajeId);
	}
	
	public void actualizarEstado(Long id, String estado) {
		jdbcTemplate.update("CALL sp_pago_actualizar_estado(?, ?)", id, estado);
	}
}
