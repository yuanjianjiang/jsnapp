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

public final class KeyStoreInitiateException extends SecurityException {
	private static final long serialVersionUID = -7801958596376478390L;

	private static final String code = "05100";
	private static final String message = "Could not initiate key store";

	public KeyStoreInitiateException(Exception cause) {
		super(code, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
