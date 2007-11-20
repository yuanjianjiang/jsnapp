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

package org.jsnap.db;

import java.sql.SQLException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.Database;
import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbInstanceTracker;
import org.postgresql.core.BaseConnection;

public final class Postgres extends Jdbc {
	static {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			Logger.getLogger(Postgres.class).log(Level.ERROR, "Could not find native JDBC driver for PostgreSQL");
		}
	}

	public Postgres(DbProperties prop, DbInstanceTracker instanceTracker) {
		super(prop, instanceTracker);
	}

	protected DbInstance getInstanceInternal() {
		return new PostgresInstance(this);
	}

	public final class PostgresInstance extends Jdbc.JdbcInstance {
		BaseConnection bconn;

		public PostgresInstance(Database owner) {
			super(owner);
		}

		protected void doConnect() throws SQLException {
			super.doConnect();
			bconn = (BaseConnection)conn;
		}

		protected void doException(SQLException e) {
			// TODO: Handle PostgreSQL specific exceptions.
			super.doException(e);
		}

		protected boolean doPing() throws SQLException {
			// DEV: Apply timeouts for ping?
			if (super.doPing()) {
				bconn.prepareStatement("SELECT 1").executeQuery().close();
				return true;
			}
			return false;
		}

		protected boolean isCritical(SQLException e) {
			// TODO: Handle PostgreSQL specific exceptions.
			return super.isCritical(e);
		}
	}
}
