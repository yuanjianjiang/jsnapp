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

public final class CredentialMismatchException extends SecurityException {
	private static final long serialVersionUID = 1161953911688534791L;

	private static final String code = "05002";
	private static final String message = "Current credentials do not match the original credentials";

	public CredentialMismatchException() {
		super(code, message);
	}

	protected Level logLevel() {
		return Level.WARN;
	}
}
