package org.jsnap.exception.security;

public final class PasswordRenewalException extends PasswordManagementException {
	private static final long serialVersionUID = -8653047971336294149L;

	private static final String code = "05001";
	private static final String message = "Password renewal failed" /* : %s */;

	public PasswordRenewalException(String errmsg) {
		super(Reason.SUPPLIED, code, message + ": " + errmsg);
	}

	public PasswordRenewalException(Exception cause) {
		super(Reason.EXCEPTION, code, message, cause);
	}
}
