package org.jsnap.exception.db;

import java.sql.SQLException;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public class SqlException extends JSnapException {
	private static final long serialVersionUID = -2300322033390583315L;

	private static final String code = "02000";
	private static final String message = "Native SQL exception";

	public SqlException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}

	protected SqlException(String code, String dbname, String message, Exception cause) {
		super(code, dbname, message, cause);
	}

	protected SqlException(String code, String dbname, String message) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
