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

import org.jsnap.exception.db.ResultSetException;

public interface DbResultSet {
	public String ownerdb();

	public boolean hasCursor();
	public int affectedRows();

	public void limitBetween(int start, int stop) throws ResultSetException;
	public void positionAt(int position) throws ResultSetException;
	public boolean next() throws ResultSetException;
	public int positionedAt();

	public Class typeMapping(int sqlType);
	public int getColumnCount();
	public DbHeader whatis(int index);
	public Object get(int index) throws ResultSetException;
	public Object get(String fieldName) throws ResultSetException;

	public void close(); 	// Calling close on an already closed DbResultSet should be a no-op. 
}
