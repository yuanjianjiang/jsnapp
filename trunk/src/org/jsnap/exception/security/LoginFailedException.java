package org.jsnap.exception.security;

public final class LoginFailedException extends PasswordManagementException {
	private static final long serialVersionUID = -6518004483637825271L;

	private static final String code = "05000";
	private static final String message = "Authentication failed" /* : %s */;

	public LoginFailedException(Reason reason, String errmsg) {
		super(reason, code, message + ": " + errmsg);
	}

	public LoginFailedException(Exception cause) {
		super(Reason.EXCEPTION, code, message, cause);
	}
}
