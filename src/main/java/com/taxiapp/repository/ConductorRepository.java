package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Conductor;

public interface ConductorRepository extends JpaRepository<Conductor, Long> {
	
	@Procedure(procedureName = "sp_conductor_buscar_por_id")
	Conductor buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_conductor_buscar_por_usuario")
	Conductor buscarPorUsuario(@Param("p_usuario_id") Long usuarioId);
	
	@Procedure(procedureName = "sp_conductor_listar_por_estado")
	List<Conductor> listarPorEstado(@Param("p_estado") String estado);

}
