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

import org.apache.log4j.Level;

public final class InstanceInactiveException extends SqlException {
	private static final long serialVersionUID = -883215500023569391L;

	private static final String code = "02013";
	private static final String message = "Database instance is not active";

	public InstanceInactiveException(String dbname) {
		super(code, dbname, message);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
