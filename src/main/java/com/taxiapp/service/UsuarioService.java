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

	@Transactional
	public Usuario actualizarPerfil(Long id, String nombre, String apellido, String email, String telefono,
			String fotoPerfil) {
		Usuario usuario = buscarPorId(id);
		if (usuario == null) {
			return null;
		}
		if (nombre != null && !nombre.isBlank()) {
			jdbcTemplate.update("UPDATE usuario SET nombre = ? WHERE id = ?", nombre.trim(), id);
		}
		if (apellido != null && !apellido.isBlank()) {
			jdbcTemplate.update("UPDATE usuario SET apellido = ? WHERE id = ?", apellido.trim(), id);
		}
		if (email != null && !email.isBlank()) {
			jdbcTemplate.update("UPDATE usuario SET email = ? WHERE id = ?", email.trim(), id);
		}
		if (telefono != null) {
			String cel = telefono.trim();
			jdbcTemplate.update("UPDATE usuario SET telefono = ? WHERE id = ?",
					cel.isEmpty() ? null : cel, id);
		}
		if (fotoPerfil != null && !fotoPerfil.isBlank()) {
			jdbcTemplate.update("UPDATE usuario SET foto_perfil = ? WHERE id = ?", fotoPerfil.trim(), id);
		}
		return buscarPorId(id);
	}

	@Transactional
	public Usuario actualizarPerfil(Long id, String nombre, String apellido, String email, String telefono,
			String fotoPerfil, String sexo, String fechaNacimiento) {
		Usuario usuario = actualizarPerfil(id, nombre, apellido, email, telefono, fotoPerfil);
		if (usuario == null) {
			return null;
		}
		try {
			if (sexo != null) {
				jdbcTemplate.update("UPDATE usuario SET sexo = ? WHERE id = ?", sexo.trim(), id);
			}
			if (fechaNacimiento != null) {
				jdbcTemplate.update("UPDATE usuario SET fecha_nacimiento = ? WHERE id = ?",
						fechaNacimiento.trim(), id);
			}
		} catch (Exception ignored) {
		}
		return buscarPorId(id);
	}
}
