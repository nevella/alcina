package com.google.gwt.dom.client;

public class LocalDomException extends RuntimeException {
	public LocalDomException() {
		super();
	}

	public LocalDomException(Throwable cause) {
		super(cause);
	}

	public LocalDomException(Throwable cause, String message) {
		super(message, cause);
	}

	public LocalDomException(String message) {
		super(message);
	}
}
