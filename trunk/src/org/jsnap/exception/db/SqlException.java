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

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public class SqlException extends JSnapException {
	private static final long serialVersionUID = -2300322033390583315L;

	private static final String code = "02000";
	private static final String message = "Native SQL exception";

	public SqlException(String dbname, SQLException cause) {
		super(code, dbname, message, cause);
	}

	protected SqlException(String code, String dbname, String message, Exception cause) {
		super(code, dbname, message, cause);
	}

	protected SqlException(String code, String dbname, String message) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
