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
