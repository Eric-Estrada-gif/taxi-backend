package com.taxiapp.exception;

public class MapsApiException extends RuntimeException {

	public MapsApiException(String message) {
		super(message);
	}

	public MapsApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
