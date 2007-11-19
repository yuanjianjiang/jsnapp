package org.jsnap.exception.db;

import java.sql.SQLException;

import org.apache.log4j.Level;

public abstract class ResultSetException extends SqlException {
	protected ResultSetException(String code, String dbname, String message, SQLException cause) {
		super(code, dbname, message, cause);
	}

	protected ResultSetException(String code, String dbname, String message) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
