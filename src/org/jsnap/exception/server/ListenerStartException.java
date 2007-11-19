package org.jsnap.exception.server;

import java.io.IOException;

import org.apache.log4j.Level;

public final class ListenerStartException extends ListenerException {
	private static final long serialVersionUID = -2299976071834479167L;

	private static final String code = "01002";
	private static final String message = "Listener could not be started";

	public ListenerStartException(int port, IOException cause) {
		super(code, port, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
