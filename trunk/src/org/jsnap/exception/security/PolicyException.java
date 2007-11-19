package org.jsnap.exception.security;

import org.apache.log4j.Level;

public final class PolicyException extends SecurityException {
	private static final long serialVersionUID = 3384109764245435650L;

	private static final String code = "05004";
	private static final String message = "Unable to set the requested security policy" /* : %s */;

	public PolicyException(String errmsg) {
		super(code, message + ": " + errmsg);
	}

	public PolicyException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
