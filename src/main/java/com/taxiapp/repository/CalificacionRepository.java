package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Calificacion;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long>{
	
	@Procedure(procedureName = "sp_calificacion_listar_por_viaje")
	List<Calificacion> listarPorViaje(@Param("p_viaje_id") Long viajeId);
	
	@Procedure(procedureName = "sp_calificacion_listar_recibidas_por_conductor")
	List<Calificacion> listarRecibidasPorConductor(@Param("p_conductor_usuario_id") Long conductorUsuarioId);

}
