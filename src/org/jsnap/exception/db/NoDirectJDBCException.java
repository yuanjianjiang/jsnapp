package org.jsnap.exception.db;

public final class NoDirectJDBCException extends DatabaseCreateException {
	private static final long serialVersionUID = -5356686640105209681L;

	private static final String code = "01999";
	private static final String message = "Use a JDBC database only indirectly with the actual driver class specified";

	public NoDirectJDBCException(String dbname) {
		super(code, dbname, message);
	}
}
