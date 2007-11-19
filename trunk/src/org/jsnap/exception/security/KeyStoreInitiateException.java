package org.jsnap.exception.security;

import org.apache.log4j.Level;

public final class KeyStoreInitiateException extends SecurityException {
	private static final long serialVersionUID = -7801958596376478390L;

	private static final String code = "05100";
	private static final String message = "Could not initiate key store";

	public KeyStoreInitiateException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
