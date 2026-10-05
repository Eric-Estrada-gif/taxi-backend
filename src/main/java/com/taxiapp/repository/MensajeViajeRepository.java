package com.taxiapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.taxiapp.entity.MensajeViaje;

public interface MensajeViajeRepository extends JpaRepository<MensajeViaje, Long>{

	List<MensajeViaje> findByViajeIdOrderByFechaEnvioAsc(Long viajeId);
}
