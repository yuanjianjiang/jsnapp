package org.jsnap.exception.db;

import java.sql.SQLException;

import org.apache.log4j.Level;

public class ConnectException extends SqlException {
	private static final long serialVersionUID = -9046258006432093810L;

	private static final String code = "02001";
	private static final String message = "Could not connect to the database";

	public ConnectException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}

	protected ConnectException(String code, String dbname, String message) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
