package com.taxiapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Vehiculo;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
	
	@Procedure(procedureName = "sp_vehiculo_buscar_por_id")
	Vehiculo buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_vehiculo_buscar_por_placa")
	Vehiculo buscarPorPlaca(@Param("p_placa") String placa);

}
