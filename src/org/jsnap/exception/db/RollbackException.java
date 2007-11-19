package org.jsnap.exception.db;

import java.sql.SQLException;

public final class RollbackException extends SqlException {
	private static final long serialVersionUID = -3678725501070018595L;

	private static final String code = "02012";
	private static final String message = "Could not rollback transaction";

	public RollbackException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}
}
