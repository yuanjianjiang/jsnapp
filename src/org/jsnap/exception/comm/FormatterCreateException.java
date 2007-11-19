package org.jsnap.exception.comm;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class FormatterCreateException extends JSnapException {
	private static final long serialVersionUID = 2591473362250478263L;

	private static final String code = "03003";
	private static final String message = "Formatter could not be created" /* : %s */;

	public FormatterCreateException(String errmsg) {
		super(code, message + ": " + errmsg);
	}

	public FormatterCreateException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
