package org.jsnap.exception.server;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class ServerConfigureException extends JSnapException {
	private static final long serialVersionUID = -1363388158465462983L;

	private static final String code = "01000";
	private static final String message = "JSnap server could not be configured";

	public ServerConfigureException(Throwable cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
