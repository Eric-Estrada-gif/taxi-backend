package com.taxiapp.service;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.taxiapp.dto.ViajeHistorialDTO;
import com.taxiapp.dto.ViajeSolicitudDTO;
import com.taxiapp.entity.Conductor;
import com.taxiapp.entity.Usuario;
import com.taxiapp.entity.Vehiculo;
import com.taxiapp.entity.Viaje;
import com.taxiapp.repository.ViajeRepository;

@Service
public class ViajeService {

	private final ViajeRepository viajeRepository;
	private final JdbcTemplate jdbcTemplate;
	private final ConductorService conductorService;
	private final UsuarioService usuarioService;
	private final VehiculoService vehiculoService;

	@Autowired
	public ViajeService(ViajeRepository viajeRepository, JdbcTemplate jdbcTemplate,
			ConductorService conductorService, UsuarioService usuarioService,
			VehiculoService vehiculoService) {
		this.viajeRepository = viajeRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.conductorService = conductorService;
		this.usuarioService = usuarioService;
		this.vehiculoService = vehiculoService;
	}

	public Long solicitarViaje(ViajeSolicitudDTO dto) {
		String sql = "{call sp_viaje_insertar(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";

		return jdbcTemplate.execute((java.sql.Connection con) -> con.prepareCall(sql),
				(CallableStatement cs) -> {
					cs.setLong(1, dto.getRiderId());
					cs.setLong(2, dto.getTarifaId());
					cs.setString(3, dto.getOrigenDireccion());
					cs.setBigDecimal(4, dto.getOrigenLat());
					cs.setBigDecimal(5, dto.getOrigenLng());
					cs.setString(6, dto.getDestinoDireccion());
					cs.setBigDecimal(7, dto.getDestinoLat());
					cs.setBigDecimal(8, dto.getDestinoLng());
					cs.setBigDecimal(9, dto.getMontoTotal());
					cs.registerOutParameter(10, Types.BIGINT);

					cs.execute();

					return cs.getLong(10);
				});
	}

	@Transactional
	public Viaje buscarPorId(Long id) {
		return viajeRepository.buscarPorId(id);
	}

	@Transactional
	public List<Viaje> listarPorRider(Long riderId) {
		return viajeRepository.listarPorRider(riderId);
	}

	@Transactional
	public List<Viaje> listarPorConductor(Long conductorId) {
		return viajeRepository.listarPorConductor(conductorId);
	}

	@Transactional
	public List<Viaje> listarSolicitados() {
		String sql = "CALL sp_viaje_listar_solicitados()";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Viaje viaje = new Viaje();
			viaje.setId(rs.getLong("id"));

			viaje.setOrigenDireccion(rs.getString("origen_direccion"));
			viaje.setOrigenLat(rs.getBigDecimal("origen_lat"));
			viaje.setOrigenLng(rs.getBigDecimal("origen_lng"));
			viaje.setDestinoDireccion(rs.getString("destino_direccion"));
			viaje.setDestinoLat(rs.getBigDecimal("destino_lat"));
			viaje.setDestinoLng(rs.getBigDecimal("destino_lng"));

			if (rs.getString("estado") != null) {
				viaje.setEstado(Viaje.Estado.valueOf(rs.getString("estado")));
			}

			viaje.setMontoTotal(rs.getBigDecimal("monto_total"));

			if (rs.getTimestamp("fecha_solicitud") != null) {
				viaje.setFechaSolicitud(rs.getTimestamp("fecha_solicitud").toLocalDateTime());
			}

			return viaje;
		});
	}

	public void aceptar(Long viajeId, Long conductorId) {
		exigirExiste(viajeId);
		String estado = obtenerEstado(viajeId);
		if (!"SOLICITADO".equals(estado)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "El viaje ya no esta disponible");
		}
		jdbcTemplate.update("CALL sp_viaje_aceptar(?, ?)", viajeId, conductorId);
	}

	public void iniciar(Long viajeId) {
		exigirExiste(viajeId);
		jdbcTemplate.update("CALL sp_viaje_iniciar(?)", viajeId);
	}

	public void finalizar(Long viajeId) {
		exigirExiste(viajeId);
		jdbcTemplate.update("CALL sp_viaje_finalizar(?)", viajeId);
	}

	public void cancelar(Long viajeId) {
		exigirExiste(viajeId);
		jdbcTemplate.update("CALL sp_viaje_cancelar(?)", viajeId);
	}

	/**
	 * El conductor declina un pedido SOLICITADO. El viaje sigue disponible
	 * para otros. Si ya lo habia aceptado, se cancela.
	 */
	public String rechazar(Long viajeId, Long conductorId) {
		exigirExiste(viajeId);
		String estado = obtenerEstado(viajeId);
		if ("SOLICITADO".equals(estado)) {
			return "RECHAZADO";
		}
		if ("ACEPTADO".equals(estado)) {
			Long asignado = obtenerConductorId(viajeId);
			if (asignado != null && asignado.equals(conductorId)) {
				cancelar(viajeId);
				return "CANCELADO";
			}
		}
		throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes rechazar este viaje en su estado actual");
	}

	public Map<String, Object> obtenerDetalleParaCliente(Long id) {
		List<Map<String, Object>> filas = jdbcTemplate.query(
				"""
						SELECT id, estado, origen_direccion, destino_direccion,
						       origen_lat, origen_lng, destino_lat, destino_lng,
						       monto_total, conductor_id, rider_id
						FROM viaje
						WHERE id = ?
						""",
				(rs, rowNum) -> {
					Map<String, Object> fila = new HashMap<>();
					fila.put("id", rs.getLong("id"));
					fila.put("viajeId", rs.getLong("id"));
					fila.put("estado", rs.getString("estado"));
					fila.put("origenDireccion", rs.getString("origen_direccion"));
					fila.put("destinoDireccion", rs.getString("destino_direccion"));
					fila.put("origenLat", rs.getBigDecimal("origen_lat"));
					fila.put("origenLng", rs.getBigDecimal("origen_lng"));
					fila.put("destinoLat", rs.getBigDecimal("destino_lat"));
					fila.put("destinoLng", rs.getBigDecimal("destino_lng"));
					fila.put("montoTotal", rs.getBigDecimal("monto_total"));
					long conductorId = rs.getLong("conductor_id");
					fila.put("conductorId", rs.wasNull() ? null : conductorId);
					fila.put("riderId", rs.getLong("rider_id"));
					return fila;
				},
				id);

		if (filas.isEmpty()) {
			return null;
		}

		Map<String, Object> detalle = filas.get(0);
		Long riderIdDetalle = (Long) detalle.get("riderId");
		if (riderIdDetalle != null) {
			Usuario rider = usuarioService.buscarPorId(riderIdDetalle);
			String nombreRider = nombreCompleto(rider);
			if (nombreRider != null) {
				detalle.put("riderNombre", nombreRider);
			}
		}
		Long conductorId = (Long) detalle.get("conductorId");
		String estado = (String) detalle.get("estado");
		if (conductorId != null) {
			try {
				detalle.putAll(armarPayloadEstado(id, estado, conductorId));
			} catch (Exception ignored) {
				detalle.put("estado", estado);
				detalle.put("conductorId", conductorId);
			}
		}

		try {
			jdbcTemplate.query(
					"""
							SELECT lat, lng
							FROM ubicacion
							WHERE viaje_id = ?
							ORDER BY timestamp DESC, id DESC
							LIMIT 1
							""",
					(rs, rowNum) -> {
						detalle.put("conductorLat", rs.getBigDecimal("lat"));
						detalle.put("conductorLng", rs.getBigDecimal("lng"));
						return null;
					},
					id);
		} catch (Exception ignored) {
		}

		return detalle;
	}

	public Long obtenerRiderId(Long viajeId) {
		try {
			return jdbcTemplate.queryForObject("SELECT rider_id FROM viaje WHERE id = ?", Long.class, viajeId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public Long obtenerConductorId(Long viajeId) {
		try {
			return jdbcTemplate.queryForObject("SELECT conductor_id FROM viaje WHERE id = ?", Long.class, viajeId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public String obtenerEstado(Long viajeId) {
		try {
			return jdbcTemplate.queryForObject("SELECT estado FROM viaje WHERE id = ?", String.class, viajeId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	public void exigirParticipante(Long viajeId, Long usuarioId, boolean admin) {
		if (admin) {
			return;
		}
		if (esRider(viajeId, usuarioId) || esConductorUsuario(viajeId, usuarioId)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres parte de este viaje");
	}

	public boolean esRider(Long viajeId, Long usuarioId) {
		Long riderId = obtenerRiderId(viajeId);
		return riderId != null && riderId.equals(usuarioId);
	}

	public boolean esConductorUsuario(Long viajeId, Long usuarioId) {
		Long conductorId = obtenerConductorId(viajeId);
		if (conductorId == null) {
			return false;
		}
		Conductor conductor = conductorService.buscarPorUsuario(usuarioId);
		return conductor != null && conductorId.equals(conductor.getId());
	}

	public Map<String, Object> armarPayloadEstado(Long viajeId, String estado, Long conductorId) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("viajeId", viajeId);
		payload.put("estado", estado);
		payload.put("conductorId", conductorId);

		if (conductorId == null) {
			return payload;
		}

		Conductor conductor = conductorService.buscarPorId(conductorId);
		if (conductor == null) {
			return payload;
		}

		Usuario usuario = conductor.getUsuario();
		if (usuario != null && usuario.getId() != null && (usuario.getNombre() == null || usuario.getNombre().isBlank())) {
			Usuario loaded = usuarioService.buscarPorId(usuario.getId());
			if (loaded != null) {
				usuario = loaded;
			}
		}

		if (usuario != null) {
			String apellido = usuario.getApellido() != null ? usuario.getApellido() : "";
			payload.put("nombreConductor", (usuario.getNombre() + " " + apellido).trim());
			payload.put("fotoPerfil", usuario.getFotoPerfil());
		}

		Vehiculo vehiculo = conductor.getVehiculo();
		if (vehiculo != null && vehiculo.getId() != null && vehiculo.getPlaca() == null) {
			Vehiculo loaded = vehiculoService.buscarPorId(vehiculo.getId());
			if (loaded != null) {
				vehiculo = loaded;
			}
		}

		if (vehiculo != null) {
			String color = vehiculo.getColor() != null ? vehiculo.getColor() : "";
			String marca = vehiculo.getMarca() != null ? vehiculo.getMarca() : "";
			String modelo = vehiculo.getModelo() != null ? vehiculo.getModelo() : "";
			String placa = vehiculo.getPlaca() != null ? vehiculo.getPlaca() : "";
			payload.put("vehiculo", (color + " " + marca + " " + modelo + " • " + placa).trim());
		}

		payload.put("calificacion", conductor.getCalificacionPromedio());
		return payload;
	}

	@Transactional(readOnly = true)
	public List<ViajeHistorialDTO> listarHistorialPorRiderDTO(Long riderId) {
		String sql = "CALL sp_viaje_historial_por_rider(?)";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ViajeHistorialDTO dto = new ViajeHistorialDTO();
			dto.setId(rs.getLong("id"));
			dto.setOrigen(rs.getString("origen_direccion"));
			dto.setDestino(rs.getString("destino_direccion"));
			dto.setEstado(rs.getString("estado"));
			dto.setCostoTotal(rs.getBigDecimal("monto_total") != null ? rs.getBigDecimal("monto_total").doubleValue() : 0.0);

			dto.setFecha(rs.getTimestamp("fecha_solicitud") != null
					? rs.getTimestamp("fecha_solicitud").toLocalDateTime().toLocalDate().toString()
					: "N/A");
			dto.setHoraInicio(rs.getTimestamp("fecha_inicio") != null
					? rs.getTimestamp("fecha_inicio").toLocalDateTime().toLocalTime().toString()
					: "--:--");
			dto.setHoraFin(rs.getTimestamp("fecha_fin") != null
					? rs.getTimestamp("fecha_fin").toLocalDateTime().toLocalTime().toString()
					: "--:--");
			dto.setCategoria("City ride");

			String nombreConductor = rs.getString("c_nombre");
			if (nombreConductor != null) {
				String apellidoConductor = rs.getString("c_apellido");
				dto.setConductorNombre(nombreConductor + " " + (apellidoConductor != null ? apellidoConductor : ""));

				String color = rs.getString("v_color");
				String marca = rs.getString("v_marca");
				String modelo = rs.getString("v_modelo");
				String placa = rs.getString("v_placa");

				String detalleAuto = (color != null ? color : "") + " "
						+ (marca != null ? marca : "") + " "
						+ (modelo != null ? modelo : "") + ", "
						+ (placa != null ? placa : "S/N");
				dto.setConductorDetalleAuto(detalleAuto.trim());

			} else {
				dto.setConductorNombre("Sin conductor asignado");
				dto.setConductorDetalleAuto("Cancelado / Pendiente");
			}

			try {
				String metodo = jdbcTemplate.queryForObject(
						"SELECT metodo FROM pago WHERE viaje_id = ?", String.class, dto.getId());
				dto.setMetodoPago(metodo);
			} catch (EmptyResultDataAccessException e) {
				dto.setMetodoPago(null);
			}

			try {
				Integer puntaje = jdbcTemplate.queryForObject(
						"SELECT puntaje FROM calificacion WHERE viaje_id = ? AND usuario_id = ? ORDER BY id DESC LIMIT 1",
						Integer.class, dto.getId(), riderId);
				dto.setCalificacionEstrellas(puntaje != null ? puntaje : 0);
			} catch (EmptyResultDataAccessException e) {
				dto.setCalificacionEstrellas(0);
			}

			return dto;
		}, riderId);
	}

	/**
	 * Historial del conductor: riderNombre sale de usuario del rider,
	 * conductorNombre del usuario del conductor. Antes el nombre del rider
	 * se guardaba por error en conductorNombre y el detalle mostraba "Pasajero".
	 */
	@Transactional(readOnly = true)
	public List<ViajeHistorialDTO> listarHistorialPorConductorDTO(Long conductorId) {
		String sql = """
				SELECT v.id,
				       v.origen_direccion,
				       v.destino_direccion,
				       v.origen_lat,
				       v.origen_lng,
				       v.destino_lat,
				       v.destino_lng,
				       v.estado,
				       v.monto_total,
				       v.fecha_solicitud,
				       v.fecha_inicio,
				       v.fecha_fin,
				       ru.nombre AS rider_nombre,
				       ru.apellido AS rider_apellido,
				       cu.nombre AS cond_nombre,
				       cu.apellido AS cond_apellido,
				       veh.color AS v_color,
				       veh.marca AS v_marca,
				       veh.modelo AS v_modelo,
				       veh.placa AS v_placa,
				       v.rider_id
				FROM viaje v
				INNER JOIN usuario ru ON ru.id = v.rider_id
				LEFT JOIN conductor c ON c.id = v.conductor_id
				LEFT JOIN usuario cu ON cu.id = c.usuario_id
				LEFT JOIN vehiculo veh ON veh.id = c.vehiculo_id
				WHERE v.conductor_id = ?
				ORDER BY v.fecha_solicitud DESC
				""";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			ViajeHistorialDTO dto = new ViajeHistorialDTO();
			dto.setId(rs.getLong("id"));
			dto.setOrigen(rs.getString("origen_direccion"));
			dto.setDestino(rs.getString("destino_direccion"));
			dto.setEstado(rs.getString("estado"));
			dto.setCostoTotal(rs.getBigDecimal("monto_total") != null ? rs.getBigDecimal("monto_total").doubleValue() : 0.0);

			dto.setFecha(rs.getTimestamp("fecha_solicitud") != null
					? rs.getTimestamp("fecha_solicitud").toLocalDateTime().toLocalDate().toString()
					: "N/A");
			dto.setHoraInicio(rs.getTimestamp("fecha_inicio") != null
					? rs.getTimestamp("fecha_inicio").toLocalDateTime().toLocalTime().toString()
					: "--:--");
			dto.setHoraFin(rs.getTimestamp("fecha_fin") != null
					? rs.getTimestamp("fecha_fin").toLocalDateTime().toLocalTime().toString()
					: "--:--");
			dto.setCategoria("City ride");
			if (rs.getBigDecimal("origen_lat") != null) dto.setOrigenLat(rs.getBigDecimal("origen_lat").doubleValue());
			if (rs.getBigDecimal("origen_lng") != null) dto.setOrigenLng(rs.getBigDecimal("origen_lng").doubleValue());
			if (rs.getBigDecimal("destino_lat") != null) dto.setDestinoLat(rs.getBigDecimal("destino_lat").doubleValue());
			if (rs.getBigDecimal("destino_lng") != null) dto.setDestinoLng(rs.getBigDecimal("destino_lng").doubleValue());

			dto.setRiderId(rs.getLong("rider_id"));
			dto.setRiderNombre(unirNombre(rs.getString("rider_nombre"), rs.getString("rider_apellido")));
			dto.setConductorNombre(unirNombre(rs.getString("cond_nombre"), rs.getString("cond_apellido")));

			String color = rs.getString("v_color");
			String marca = rs.getString("v_marca");
			String modelo = rs.getString("v_modelo");
			String placa = rs.getString("v_placa");
			String detalleAuto = (color != null ? color : "") + " "
					+ (marca != null ? marca : "") + " "
					+ (modelo != null ? modelo : "") + ", "
					+ (placa != null ? placa : "S/N");
			dto.setConductorDetalleAuto(detalleAuto.trim());

			try {
				String metodo = jdbcTemplate.queryForObject(
						"SELECT metodo FROM pago WHERE viaje_id = ?", String.class, dto.getId());
				dto.setMetodoPago(metodo);
			} catch (EmptyResultDataAccessException e) {
				dto.setMetodoPago(null);
			}

			try {
				Integer puntaje = jdbcTemplate.queryForObject(
						"SELECT puntaje FROM calificacion WHERE viaje_id = ? AND usuario_id = ? ORDER BY id DESC LIMIT 1",
						Integer.class, dto.getId(), rs.getLong("rider_id"));
				dto.setCalificacionEstrellas(puntaje != null ? puntaje : 0);
			} catch (EmptyResultDataAccessException e) {
				dto.setCalificacionEstrellas(0);
			}

			return dto;
		}, conductorId);
	}

	private String nombreCompleto(Usuario usuario) {
		if (usuario == null) {
			return null;
		}
		return unirNombre(usuario.getNombre(), usuario.getApellido());
	}

	private String unirNombre(String nombre, String apellido) {
		String n = nombre != null ? nombre.trim() : "";
		String a = apellido != null ? apellido.trim() : "";
		String full = (n + " " + a).trim();
		return full.isEmpty() ? null : full;
	}

	private void exigirExiste(Long viajeId) {
		if (obtenerEstado(viajeId) == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado");
		}
	}
}
