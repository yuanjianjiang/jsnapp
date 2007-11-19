package org.jsnap.exception.comm;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class CommunicationException extends JSnapException {
	private static final long serialVersionUID = 2123282568368280800L;

	private static final String code = "03000";
	private static final String message = "Exception in the underlying communication protocol";

	public CommunicationException(IOException e) {
		super(code, message, e);
	}

	public CommunicationException(HttpException e) {
		super(code, message, e);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
