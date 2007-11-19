package org.jsnap.exception.db;

import org.apache.log4j.Level;

public final class RolledbackException extends AccessKeyException {
	private static final long serialVersionUID = -2275717534948808345L;

	private static final String code = "02202";
	private static final String message = "Transaction had been rolled back";

	public RolledbackException() {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
