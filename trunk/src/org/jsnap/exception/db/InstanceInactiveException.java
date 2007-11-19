package org.jsnap.exception.db;

import org.apache.log4j.Level;

public final class InstanceInactiveException extends SqlException {
	private static final long serialVersionUID = -883215500023569391L;

	private static final String code = "02013";
	private static final String message = "Database instance is not active";

	public InstanceInactiveException(String dbname) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
