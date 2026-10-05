package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Viaje;

public interface ViajeRepository extends JpaRepository<Viaje, Long>{
	
	@Procedure(procedureName = "sp_viaje_buscar_por_id")
	Viaje buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_viaje_listar_por_rider")
	List<Viaje> listarPorRider(@Param("p_rider_id") Long riderId);
	
	@Procedure(procedureName = "sp_viaje_listar_por_conductor")
	List<Viaje> listarPorConductor(@Param("p_conductor_id") Long conductorId);
	
	@Procedure(procedureName = "sp_viaje_listar_solicitando")
	List<Viaje> listarSolicitando();
}
