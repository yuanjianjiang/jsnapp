package org.jsnap.exception;

import org.apache.log4j.Level;

public final class UnhandledException extends JSnapException {
	private static final long serialVersionUID = -8544563152270410582L;

	private static final String code = "00999";
	private static final String message = "A throwable was not properly handled";

	public UnhandledException(Throwable cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
