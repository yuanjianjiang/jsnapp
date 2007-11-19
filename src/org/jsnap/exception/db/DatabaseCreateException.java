package org.jsnap.exception.db;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public class DatabaseCreateException extends JSnapException {
	private static final long serialVersionUID = 874946262624951456L;

	private static final String code = "01998";
	private static final String message = "Database could not be created" /* : %s */;

	public DatabaseCreateException(String dbname, String errmsg) {
		super(code, dbname, message + ": " + errmsg);
	}

	public DatabaseCreateException(String dbname, Exception cause) {
		super(code, dbname, message, cause);
	}

	protected DatabaseCreateException(String code, String dbname, String message) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
