package org.jsnap.exception.comm;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class MalformedResponseException extends JSnapException {
	private static final long serialVersionUID = 1253415264309220106L;

	private static final String code = "03002";
	private static final String message = "Malformed response: " /* %s */;

	public MalformedResponseException(String errmsg) {
		super(code, message + errmsg);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
