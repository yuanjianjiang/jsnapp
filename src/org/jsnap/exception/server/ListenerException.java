package org.jsnap.exception.server;

import org.jsnap.exception.JSnapException;

public abstract class ListenerException extends JSnapException {
	protected ListenerException(String code, int port, String message, Throwable cause) {
		super(code, Integer.toString(port), message, cause);
	}

	protected ListenerException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}

	protected ListenerException(String code, String message) {
		super(code, message);
	}
}
