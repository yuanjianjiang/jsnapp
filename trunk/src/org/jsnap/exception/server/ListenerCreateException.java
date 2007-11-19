package org.jsnap.exception.server;

import org.apache.log4j.Level;

public final class ListenerCreateException extends ListenerException {
	private static final long serialVersionUID = -6394279820465439898L;

	private static final String code = "01001";
	private static final String message = "Listener could not be created" /* : %s */;

	public ListenerCreateException(String errmsg) {
		super(code, message + ": " + errmsg);
	}

	public ListenerCreateException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
