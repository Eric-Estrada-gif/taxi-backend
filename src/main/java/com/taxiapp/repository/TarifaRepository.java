package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Tarifa;

public interface TarifaRepository extends JpaRepository<Tarifa, Long>{
	@Procedure(procedureName = "sp_tarifa_buscar_por_id")
	Tarifa buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_tarifa_listar_activas")
	List<Tarifa> listarActivas();

}
