package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Calificacion;
import com.taxiapp.repository.CalificacionRepository;

@Service
public class CalificacionService {

	private final CalificacionRepository calificacionRepository;
    private final JdbcTemplate jdbcTemplate;
 
    @Autowired
    public CalificacionService(CalificacionRepository calificacionRepository, JdbcTemplate jdbcTemplate) {
        this.calificacionRepository = calificacionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }
 
    public Long registrarCalificacion(Long viajeId, Long usuarioId, Integer puntaje, String comentario) {
 
        String sql = "{call sp_calificacion_insertar(?, ?, ?, ?, ?)}";
 
        return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setLong(1, viajeId);
                    cs.setLong(2, usuarioId);
                    cs.setByte(3, puntaje.byteValue());
                    cs.setString(4, comentario);
                    cs.registerOutParameter(5, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(5);
                });
    }
 
    @Transactional
    public List<Calificacion> listarPorViaje(Long viajeId) {
        return calificacionRepository.listarPorViaje(viajeId);
    }
 
    @Transactional
    public List<Calificacion> listarRecibidasPorConductor(Long conductorUsuarioId) {
        return calificacionRepository.listarRecibidasPorConductor(conductorUsuarioId);
    }
}
