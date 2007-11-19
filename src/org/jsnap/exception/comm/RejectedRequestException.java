package org.jsnap.exception.comm;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class RejectedRequestException extends JSnapException {
	private static final long serialVersionUID = -8709634913383086295L;

	private static final String code = "03100";
	private static final String message = "Server was busy and rejected to execute the request";

	public RejectedRequestException() {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
