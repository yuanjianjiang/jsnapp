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

package org.jsnap.db.base;

import java.sql.SQLException;

import org.jsnap.exception.db.SqlException;
import org.jsnap.util.JPair;

public interface DbStatement {
	public JPair<Integer, Object> typeMapping(DbParam param) throws SQLException;
	// DEV: Implement a better timeout mechanism than JDBC's.
	public void setTimeout(long timeout) throws SqlException;
	public void setMaxRows(int maxRows) throws SqlException;
	public DbResultSet execute() throws SqlException;
}
