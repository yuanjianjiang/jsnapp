package org.jsnap.exception.db;

public final class ResultSetNotScrollableException extends ResultSetException {
	private static final long serialVersionUID = 2254716406867564167L;

	private static final String code = "02008";
	private static final String message = "Result set is not scrollable";

	public ResultSetNotScrollableException(String dbname) {
		super(code, dbname, message);
	}
}
