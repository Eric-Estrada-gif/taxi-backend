package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Ubicacion;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {
	
	@Procedure(procedureName = "sp_ubicacion_listar_por_viaje")
	List<Ubicacion> listarPorVaje(@Param("p_viaje_id") Long viajeId);
	
	@Procedure(procedureName = "sp_ubicacion_obtener_ultima")
	Ubicacion obtenerUltima(@Param("p_viaje_id") Long viajeId);
}
