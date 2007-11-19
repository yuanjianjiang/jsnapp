package org.jsnap.exception.db;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public class AccessKeyException extends JSnapException {
	private static final long serialVersionUID = -291697302923971820L;

	private static final String code = "02200";
	private static final String message = "Access key does not map to a stored response";

	public AccessKeyException() {
		super(code, message);
	}

	protected AccessKeyException(String code, String message) {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
