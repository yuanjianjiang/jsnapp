package org.jsnap.exception.db;

import java.sql.SQLException;

public final class ResultSetReadException extends ResultSetException {
	private static final long serialVersionUID = -875743042587334583L;

	private static final String code = "02009";
	private static final String message = "Could not read field from result set" /* : %s */;

	public ResultSetReadException(String dbname, String errmsg) {
		super(code, dbname, message + ": " + errmsg);
	}

	public ResultSetReadException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}
}
