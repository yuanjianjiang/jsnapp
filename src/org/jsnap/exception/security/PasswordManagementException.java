/************************************************************************
 * This file is part of jsnap.                                          *
 *                                                                      *
 * jsnap is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation, either version 3 of the License, or    *
 * (at your option) any later version.                                  *
 *                                                                      *
 * jsnap is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 * GNU General Public License for more details.                         *
 *                                                                      *
 * You should have received a copy of the GNU General Public License    *
 * along with jsnap.  If not, see <http://www.gnu.org/licenses/>.       *
 ************************************************************************/

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
