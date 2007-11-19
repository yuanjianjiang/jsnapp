package org.jsnap.exception.db;

public final class UnknownNameException extends ConnectException {
	private static final long serialVersionUID = -1968967821403035317L;

	private static final String code = "02003";
	private static final String message = "Database name is not recognized";

	public UnknownNameException(String dbname) {
		super(code, dbname, message);
	}
}
