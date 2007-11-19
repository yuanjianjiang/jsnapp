package org.jsnap.exception.security;

import org.apache.log4j.Level;

public abstract class PasswordManagementException extends SecurityException {
	public final Reason reason;

	public enum Reason { SUCCESS, NO_SUCH_USER, LOCKED_OUT, RENEW_PASSWORD, INCORRECT_PASSWORD, EXCEPTION, SUPPLIED };

	public static String reasonText(int reason) {
		// Maps Reason ordinals to text messages.
		switch (reason) {
		case 0: return "Successful";
		case 1: return "User does not exist";
		case 2: return "User locked out";
		case 3: return "Expired password";
		case 4: return "Incorrect password";
		case 5: return "Java Exception";
		case 6: return "Other";
		default: return "Incorrect Reason Code";
		}
	}

	protected PasswordManagementException(Reason reason, String code, String message) {
		super(code, message);
		this.reason = reason;
	}

	protected PasswordManagementException(Reason reason, String code, String message, Exception cause) {
		super(code, message, cause);
		this.reason = reason;
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
