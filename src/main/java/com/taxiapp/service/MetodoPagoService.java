package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.MetodoPago;
import com.taxiapp.repository.MetodoPagoRepository;

@Service
public class MetodoPagoService {
	
	private final MetodoPagoRepository metodoPagoRepository;
    private final JdbcTemplate jdbcTemplate;
 
    @Autowired
    public MetodoPagoService(MetodoPagoRepository metodoPagoRepository, JdbcTemplate jdbcTemplate) {
        this.metodoPagoRepository = metodoPagoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
 
    public Long registrarMetodoPago(Long usuarioId, String tipo, String detalle, Boolean predeterminado) {
 
        String sql = "{call sp_metodo_pago_insertar(?, ?, ?, ?, ?)}";
 
        return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setLong(1, usuarioId);
                    cs.setString(2, tipo);
                    cs.setString(3, detalle);
                    cs.setBoolean(4, predeterminado != null && predeterminado);
                    cs.registerOutParameter(5, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(5);
                });
    }
 
    @Transactional
    public List<MetodoPago> listarPorUsuario(Long usuarioId) {
        return metodoPagoRepository.listarPorUsuario(usuarioId);
    }
 
    public void eliminar(Long id) {
        jdbcTemplate.update("CALL sp_metodo_pago_eliminar(?)", id);
    }
}
