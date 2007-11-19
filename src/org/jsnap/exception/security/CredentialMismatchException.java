package org.jsnap.exception.security;

import org.apache.log4j.Level;

public final class CredentialMismatchException extends SecurityException {
	private static final long serialVersionUID = 1161953911688534791L;

	private static final String code = "05002";
	private static final String message = "Current credentials do not match the original credentials";

	public CredentialMismatchException() {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
