package org.jsnap.exception.db;

import org.jsnap.db.base.Dbregistry;

public final class InternalDbUnavailable extends ConnectException {
	private static final long serialVersionUID = 7971657120955653878L;

	private static final String code = "02006";
	private static final String message = "Internal database is not available";

	public InternalDbUnavailable() {
		super(code, Dbregistry.INTERNALDB_NAME, message);
	}
}
