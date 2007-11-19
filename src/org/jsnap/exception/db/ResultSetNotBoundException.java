package org.jsnap.exception.db;

public final class ResultSetNotBoundException extends ResultSetException {
	private static final long serialVersionUID = 3329119326488743547L;

	private static final String code = "02007";
	private static final String message = "Result set is not bound to an underlying object";

	public ResultSetNotBoundException(String dbname) {
		super(code, dbname, message);
	}
}
