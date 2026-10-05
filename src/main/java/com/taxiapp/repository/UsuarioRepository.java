package com.taxiapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.taxiapp.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
	
	// Select que devuelve una entidad completa: @Procedure funciona directo
	@Procedure(procedureName = "sp_usuario_buscar_por_id")
	Optional<Usuario> buscarPorId(@Param("p_id") Long id);
	
	@Procedure(procedureName = "sp_usuario_buscar_por_email")
	Optional<Usuario> buscarPorEmail(@Param("p_email") String email);
	
	@Procedure(procedureName = "sp_usuario_listar_por_rol")
	List<Usuario> listarPorRol(@Param("p_rol") String rol);
	
	@Procedure(procedureName = "sp_usuario_buscar_por_google_id")
	Usuario buscarPorGoogleId(@Param("p_google_id") String googleId);
	
	// Los SP con parametros OUT (como sp_usuario_insertar) no se manejan
    // bien con @Procedure -> se resuelven en el Service con SimpleJdbcCall
    // (ver UsuarioService.java)
}
