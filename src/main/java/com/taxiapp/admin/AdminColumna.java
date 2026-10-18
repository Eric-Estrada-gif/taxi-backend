package com.taxiapp.admin;

public record AdminColumna(
		String nombre,
		String etiqueta,
		String tipo,
		boolean pk,
		boolean editable,
		boolean enLista) {

	public String tipoBase() {
		if (tipo != null && tipo.startsWith("select:")) {
			return "select";
		}
		return tipo;
	}
}
