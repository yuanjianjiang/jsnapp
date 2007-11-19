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
