package com.taxiapp.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminPortalController {

	private final AdminCrudService crud;

	public AdminPortalController(AdminCrudService crud) {
		this.crud = crud;
	}

	@GetMapping("/login")
	public String login() {
		return "admin/login";
	}

	@GetMapping
	public String inicio(Model model) {
		Map<String, Long> conteos = new LinkedHashMap<>();
		for (AdminTabla tabla : crud.tablas()) {
			try {
				conteos.put(tabla.clave(), crud.contar(tabla));
			} catch (Exception e) {
				conteos.put(tabla.clave(), -1L);
			}
		}
		model.addAttribute("tablas", crud.tablas());
		model.addAttribute("conteos", conteos);
		return "admin/inicio";
	}

	@GetMapping("/{clave}")
	public String lista(@PathVariable String clave, Model model, RedirectAttributes ra) {
		try {
			AdminTabla tabla = AdminTabla.porClave(clave);
			model.addAttribute("tabla", tabla);
			model.addAttribute("filas", crud.listar(tabla));
			model.addAttribute("tablas", crud.tablas());
			return "admin/lista";
		} catch (Exception e) {
			ra.addFlashAttribute("error", e.getMessage());
			return "redirect:/admin";
		}
	}

	@GetMapping("/{clave}/nuevo")
	public String nuevo(@PathVariable String clave, Model model) {
		AdminTabla tabla = AdminTabla.porClave(clave);
		model.addAttribute("tabla", tabla);
		model.addAttribute("fila", crud.filaVacia(tabla));
		model.addAttribute("nuevo", true);
		model.addAttribute("tablas", crud.tablas());
		model.addAttribute("crud", crud);
		return "admin/form";
	}

	@GetMapping("/{clave}/{id}/editar")
	public String editar(@PathVariable String clave, @PathVariable String id, Model model, RedirectAttributes ra) {
		AdminTabla tabla = AdminTabla.porClave(clave);
		Map<String, Object> fila = crud.buscar(tabla, id);
		if (fila == null) {
			ra.addFlashAttribute("error", "No se encontró el registro");
			return "redirect:/admin/" + clave;
		}
		model.addAttribute("tabla", tabla);
		model.addAttribute("fila", fila);
		model.addAttribute("nuevo", false);
		model.addAttribute("idActual", id);
		model.addAttribute("tablas", crud.tablas());
		model.addAttribute("crud", crud);
		return "admin/form";
	}

	@PostMapping("/{clave}/guardar")
	public String guardar(@PathVariable String clave, @RequestParam(required = false) String idActual,
			HttpServletRequest request, RedirectAttributes ra) {
		AdminTabla tabla = AdminTabla.porClave(clave);
		Map<String, String> form = new LinkedHashMap<>();
		request.getParameterMap().forEach((k, v) -> form.put(k, v != null && v.length > 0 ? v[0] : ""));
		try {
			crud.guardar(tabla, form, idActual);
			ra.addFlashAttribute("ok", "Registro guardado");
		} catch (Exception e) {
			ra.addFlashAttribute("error", "No se pudo guardar: " + e.getMessage());
			if (idActual == null || idActual.isBlank()) {
				return "redirect:/admin/" + clave + "/nuevo";
			}
			return "redirect:/admin/" + clave + "/" + idActual + "/editar";
		}
		return "redirect:/admin/" + clave;
	}

	@PostMapping("/{clave}/{id}/eliminar")
	public String eliminar(@PathVariable String clave, @PathVariable String id, RedirectAttributes ra) {
		AdminTabla tabla = AdminTabla.porClave(clave);
		try {
			crud.eliminar(tabla, id);
			ra.addFlashAttribute("ok", "Registro eliminado");
		} catch (Exception e) {
			ra.addFlashAttribute("error", "No se pudo eliminar (puede tener datos relacionados): " + e.getMessage());
		}
		return "redirect:/admin/" + clave;
	}
}
