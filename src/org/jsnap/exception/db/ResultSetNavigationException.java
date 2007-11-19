package org.jsnap.exception.db;

import java.sql.SQLException;

public final class ResultSetNavigationException extends ResultSetException {
	private static final long serialVersionUID = -5671812538178383927L;

	private static final String code = "02010";
	private static final String message = "Could not navigate within result set";

	public ResultSetNavigationException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}
}
