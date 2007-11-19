package org.jsnap.exception.server;

import java.io.IOException;

import org.apache.log4j.Level;

public final class ListenerIOException extends ListenerException {
	private static final long serialVersionUID = -2637519878530578392L;

	private static final String code = "01003";
	private static final String message = "Listener encountered an I/O exception";

	public ListenerIOException(int port, IOException cause) {
		super(code, port, message, cause);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
