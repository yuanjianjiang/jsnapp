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
