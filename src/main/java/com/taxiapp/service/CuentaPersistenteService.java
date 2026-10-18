package com.taxiapp.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.taxiapp.dto.AuthResponseDTO;

@Service
public class CuentaPersistenteService {

	private final JdbcTemplate jdbcTemplate;

	public CuentaPersistenteService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void completar(AuthResponseDTO dto) {
		if (dto == null || dto.getUsuarioId() == null) {
			return;
		}
		Map<String, Object> pago = leerPago(dto.getUsuarioId());
		dto.setMetodoPago(texto(pago.get("metodoPago")));
		dto.setPagoNumero(texto(pago.get("pagoNumero")));
		dto.setPagoVence(texto(pago.get("pagoVence")));
		dto.setPagoCvv(texto(pago.get("pagoCvv")));
		dto.setPagoYape(texto(pago.get("pagoYape")));

		Map<String, Object> vehiculo = leerVehiculo(dto.getUsuarioId());
		dto.setLicenciaNumero(texto(vehiculo.get("licenciaNumero")));
		dto.setVehiculoPlaca(texto(vehiculo.get("vehiculoPlaca")));
		dto.setVehiculoMarca(texto(vehiculo.get("vehiculoMarca")));
		dto.setVehiculoModelo(texto(vehiculo.get("vehiculoModelo")));
		dto.setVehiculoColor(texto(vehiculo.get("vehiculoColor")));
		dto.setVehiculoAnio(texto(vehiculo.get("vehiculoAnio")));
	}

	public void completarMapa(Map<String, Object> respuesta, Long usuarioId) {
		if (respuesta == null || usuarioId == null) {
			return;
		}
		respuesta.putAll(leerPago(usuarioId));
		respuesta.putAll(leerVehiculo(usuarioId));
	}

	public Map<String, Object> leerPago(Long usuarioId) {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("metodoPago", "EFECTIVO");
		try {
			List<Map<String, Object>> filas = jdbcTemplate.queryForList(
					"""
							SELECT tipo, detalle
							FROM metodo_pago
							WHERE usuario_id = ?
							ORDER BY predeterminado DESC, id DESC
							LIMIT 1
							""",
					usuarioId);
			if (filas.isEmpty()) {
				return out;
			}
			out.putAll(decodificarDetalle(String.valueOf(filas.get(0).get("tipo")),
					filas.get(0).get("detalle") == null ? "" : filas.get(0).get("detalle").toString()));
		} catch (Exception ignored) {
		}
		return out;
	}

	public Map<String, Object> leerVehiculo(Long usuarioId) {
		Map<String, Object> out = new LinkedHashMap<>();
		try {
			List<Map<String, Object>> filas = jdbcTemplate.queryForList(
					"""
							SELECT c.numero_licencia,
							       v.placa, v.marca, v.modelo, v.color, v.anio
							FROM conductor c
							LEFT JOIN vehiculo v ON v.id = c.vehiculo_id
							WHERE c.usuario_id = ?
							LIMIT 1
							""",
					usuarioId);
			if (filas.isEmpty()) {
				return out;
			}
			Map<String, Object> fila = filas.get(0);
			out.put("licenciaNumero", texto(fila.get("numero_licencia")));
			out.put("vehiculoPlaca", texto(fila.get("placa")));
			out.put("vehiculoMarca", texto(fila.get("marca")));
			String marca = texto(fila.get("marca"));
			String modelo = texto(fila.get("modelo"));
			String modeloVisible = (marca + " " + (modelo == null ? "" : modelo)).trim();
			out.put("vehiculoModelo", modeloVisible.isBlank() ? modelo : modeloVisible);
			out.put("vehiculoColor", texto(fila.get("color")));
			out.put("vehiculoAnio", fila.get("anio") == null ? null : String.valueOf(fila.get("anio")));
		} catch (Exception ignored) {
		}
		return out;
	}

	public void guardarPago(Long usuarioId, String metodo, String numero, String vence, String cvv, String yape) {
		if (usuarioId == null) {
			return;
		}
		String metodoNorm = metodo == null || metodo.isBlank() ? "EFECTIVO" : metodo.trim().toUpperCase();
		String tipoDb = "BILLETERA_DIGITAL".equals(metodoNorm) ? "BILLETERA_DIGITAL" : "TARJETA";
		String detalle = "v1|" + metodoNorm + "|"
				+ nvl(numero) + "|"
				+ nvl(vence) + "|"
				+ nvl(cvv) + "|"
				+ nvl(yape);
		if (detalle.length() > 100) {
			detalle = detalle.substring(0, 100);
		}
		jdbcTemplate.update("DELETE FROM metodo_pago WHERE usuario_id = ?", usuarioId);
		jdbcTemplate.update(
				"""
						INSERT INTO metodo_pago (usuario_id, tipo, detalle, predeterminado)
						VALUES (?, ?, ?, TRUE)
						""",
				usuarioId, tipoDb, detalle);
	}

	private Map<String, Object> decodificarDetalle(String tipo, String detalle) {
		Map<String, Object> out = new LinkedHashMap<>();
		String d = detalle == null ? "" : detalle.trim();
		if (d.startsWith("v1|")) {
			String[] p = d.split("\\|", -1);
			String metodo = p.length > 1 ? p[1] : "EFECTIVO";
			out.put("metodoPago", metodo.isBlank() ? "EFECTIVO" : metodo);
			out.put("pagoNumero", p.length > 2 ? vacioANull(p[2]) : null);
			out.put("pagoVence", p.length > 3 ? vacioANull(p[3]) : null);
			out.put("pagoCvv", p.length > 4 ? vacioANull(p[4]) : null);
			out.put("pagoYape", p.length > 5 ? vacioANull(p[5]) : null);
			return out;
		}
		if ("BILLETERA_DIGITAL".equalsIgnoreCase(tipo)) {
			out.put("metodoPago", "BILLETERA_DIGITAL");
			out.put("pagoYape", vacioANull(d.replaceAll("\\D", "")));
		} else if (d.replaceAll("\\D", "").length() >= 12) {
			out.put("metodoPago", "TARJETA");
			out.put("pagoNumero", d.replaceAll("\\D", ""));
		} else {
			out.put("metodoPago", "EFECTIVO");
		}
		return out;
	}

	private String texto(Object valor) {
		if (valor == null) {
			return null;
		}
		String t = valor.toString().trim();
		return t.isEmpty() || "null".equalsIgnoreCase(t) ? null : t;
	}

	private String nvl(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private String vacioANull(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}
}
