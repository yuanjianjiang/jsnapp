package org.jsnap.exception.db;

import java.sql.SQLException;

public final class CommitException extends SqlException {
	private static final long serialVersionUID = -522815391581720842L;

	private static final String code = "02011";
	private static final String message = "Could not commit transaction";

	public CommitException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}
}
