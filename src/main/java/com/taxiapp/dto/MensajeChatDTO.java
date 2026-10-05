package com.taxiapp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MensajeChatDTO {

	private Long viajeId;
	private String emisor;
	private String texto;
	private String hora;
}
