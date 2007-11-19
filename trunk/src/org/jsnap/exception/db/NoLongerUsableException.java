package org.jsnap.exception.db;

public final class NoLongerUsableException extends ConnectException {
	private static final long serialVersionUID = 5205863741864823888L;

	private static final String code = "02002";
	private static final String message = "Database object is no longer usable";

	public NoLongerUsableException(String dbname) {
		super(code, dbname, message);
	}
}
