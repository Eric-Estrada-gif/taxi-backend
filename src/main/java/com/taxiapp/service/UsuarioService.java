package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taxiapp.entity.Usuario;
import com.taxiapp.repository.UsuarioRepository;

@Service
public class UsuarioService {
	
	private final UsuarioRepository usuarioRepository;
	private final JdbcTemplate jdbcTemplate;
	private final PasswordEncoder passwordEncoder;
	
	@Autowired
	public UsuarioService(UsuarioRepository usuarioRepository, JdbcTemplate jdbcTemplate,
			PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.passwordEncoder = passwordEncoder;
	}
	
		// Registrar un nuevo usuario llamado al Sp sp_usuario_insertar
		// Usa CallableStatement directo (mas confiable que SimpleJdbcCall
		// para parametros OUT con el driver de MYSQL
		
	public Long registrarUsuario(String nombre, String apellido, String email,
			String passwordPlano, String telefono, String rol) {
		String sql = "{call sp_usuario_insertar(?, ?, ?, ?, ?, ?, ?)}";
		String passwordHash = passwordEncoder.encode(passwordPlano);
		
		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
                (CallableStatement cs) -> {
                    cs.setString(1, nombre);
                    cs.setString(2, apellido);
                    cs.setString(3, email);
                    cs.setString(4, passwordHash);
                    cs.setString(5, telefono);
                    cs.setString(6, rol);
                    cs.registerOutParameter(7, Types.BIGINT);
 
                    cs.execute();
 
                    return cs.getLong(7);
                });
	}
	
	@Transactional
	public Usuario buscarPorId(Long id) {
	    return usuarioRepository.buscarPorId(id).orElse(null);
	}

	@Transactional
	public Usuario buscarPorEmail(String email) {
	    return usuarioRepository.buscarPorEmail(email).orElse(null);
	}
	
	@Transactional
	public List<Usuario> listarPorRol(String rol) {
		return usuarioRepository.listarPorRol(rol);
	}
}
