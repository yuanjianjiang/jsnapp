package org.jsnap.exception.security;

import org.apache.log4j.Level;

public final class AccessDeniedException extends SecurityException {
	private static final long serialVersionUID = 7553833181802201178L;

	private static final String code = "05003";
	private static final String message = "Access to the database is denied" /* : %s */;

	public AccessDeniedException(String errmsg) {
		super(code, message + ": " + errmsg);
	}

	public AccessDeniedException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
