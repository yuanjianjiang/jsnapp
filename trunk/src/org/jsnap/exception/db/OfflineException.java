package org.jsnap.exception.db;

public final class OfflineException extends ConnectException {
	private static final long serialVersionUID = 796946079301792609L;

	private static final String code = "02004";
	private static final String message = "Database is currently offline";

	public OfflineException(String dbname) {
		super(code, dbname, message);
	}
}
