package org.jsnap.exception.comm;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class MalformedRequestException extends JSnapException {
	private static final long serialVersionUID = -8578580564086424020L;

	private static final String code = "03001";
	private static final String message = "Malformed request: " /* %s */;

	public MalformedRequestException(String errmsg) {
		super(code, message + errmsg);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
