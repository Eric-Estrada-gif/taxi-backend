package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.MetodoPago;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long>{
	
	@Procedure(procedureName = "sp_metodo_pago_listar_por_usuario")
	List<MetodoPago> listarPorUsuario(@Param("p_usuario_id") Long usuarioId);
}
