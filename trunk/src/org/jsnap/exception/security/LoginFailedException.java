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
