package com.taxiapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long>{
	
	@Procedure(procedureName = "sp_pago_buscar_por_id")
	Pago buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_pago_buscar_por_viaje")
	Pago buscarPorViaje(@Param("p_viaje_id") Long viajeId);

}
