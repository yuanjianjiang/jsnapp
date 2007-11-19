package org.jsnap.exception.security;

import org.jsnap.exception.JSnapException;

public abstract class SecurityException extends JSnapException {
	protected SecurityException(String code, String message) {
		super(code, message);
	}

	protected SecurityException(String code, String message, Exception cause) {
		super(code, message, cause);
	}
}
