package org.jsnap.exception.db;

import org.apache.log4j.Level;

public final class CommittedException extends AccessKeyException {
	private static final long serialVersionUID = -2275717534948808345L;

	private static final String code = "02201";
	private static final String message = "Transaction had been committed";

	public CommittedException() {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
