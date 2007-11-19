package org.jsnap.exception.db;

public final class TimeoutException extends ConnectException {
	private static final long serialVersionUID = 1152023046572348552L;

	private static final String code = "02005";
	private static final String message = "No database instance was available within the timeout period";

	public TimeoutException(String dbname) {
		super(code, dbname, message);
	}
}
