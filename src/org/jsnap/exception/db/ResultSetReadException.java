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

import java.sql.SQLException;

public final class ResultSetReadException extends ResultSetException {
	private static final long serialVersionUID = -875743042587334583L;

	private static final String code = "02009";
	private static final String message = "Could not read field from result set" /* : %s */;

	public ResultSetReadException(String dbname, String errmsg) {
		super(code, dbname, message + ": " + errmsg);
	}

	public ResultSetReadException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}
}
