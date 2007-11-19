package org.jsnap.db.base;

import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.db.CommitException;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.db.InstanceInactiveException;
import org.jsnap.exception.db.OfflineException;
import org.jsnap.exception.db.RollbackException;
import org.jsnap.exception.db.SqlException;

public abstract class DbInstance {
	private Database owner;
	private boolean active;
	private boolean connected;
	private long lastActive;

	public DbInstance(Database owner) {
		this.owner = owner;
		this.active = false;
		this.connected = false;
		this.lastActive = System.currentTimeMillis();
	}

	public String getOwnerName() {
		return owner.prop.name;
	}

	public long getLastActive() {
		return (active ? System.currentTimeMillis() : lastActive);
	}

	public void close() { // Calling close on an already closed instance is a no-op.
		if (active) {
			try {
				doReset();
			} catch (SQLException ignore) {
				Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an SQL exception during reset", ignore);
			}
			lastActive = System.currentTimeMillis();
			active = false;
			owner.markInstanceAsIdle(this);
			owner.returnInstance(this);
		}
	}

	public synchronized boolean connect() throws ConnectException {
		close(); // Closes if only the instance is currently active, it is a no-op otherwise.
		boolean reset = false;
		if (connected == false || ping() == false) {
			try {
				if (connected) {
					reset = true;
					disconnect(false);
				}
				connectInternal();
			} catch (ConnectException e) {
				owner.returnInstance(this); // Returns instance to the database pool.
				throw e;
			}
		}
		connected = true;
		active = true;
		return reset;
	}

	private void connectInternal() throws ConnectException {
		if (owner.offline()) {
			throw new OfflineException(owner.prop.name);
		} else {
			try {
				doConnect();
				Logger.getLogger(this.getClass()).log(Level.INFO, "Connected to " + owner.prop.name);
				owner.takeOnline();
			} catch (SQLException e) {
				ConnectException x = new ConnectException(owner.prop.name, e);
				exception(e);
				throw x;
			}
		}
	}

	public void disconnect(boolean log) {
		if (connected) {
			doDisconnect();
			if (log)
				Logger.getLogger(this.getClass()).log(Level.INFO, "Disconnected from " + owner.prop.name);
			connected = false;
		}
	}

	public DbStatement createStatement(String sql, ArrayList<DbParam> parameters, boolean scrollable) throws SqlException {
		if (active == false)
			throw new InstanceInactiveException(owner.prop.name);
		return doCreateStatement(sql, parameters, scrollable);
	}

	protected abstract DbStatement doCreateStatement(String sql, ArrayList<DbParam> parameters, boolean scrollable) throws SqlException;

	public void commit() throws CommitException, InstanceInactiveException {
		if (active) {
			try {
				doCommit();
			} catch (SQLException e) {
				exception(e);
				throw new CommitException(owner.prop.name, e);
			}
		} else {
			throw new InstanceInactiveException(owner.prop.name);
		}
	}

	public void rollback() throws RollbackException, InstanceInactiveException {
		if (active) {
			try {
				doRollback();
			} catch (SQLException e) {
				exception(e);
				throw new RollbackException(owner.prop.name, e);
			}
		} else {
			throw new InstanceInactiveException(owner.prop.name);
		}
	}

	protected boolean ping() {
		if (owner.offline())
			return false;
		try {
			return doPing();
		} catch (SQLException e) {
			exception(e);
			return false;
		}
	}

	protected void exception(SQLException e) {
		if (isCritical(e))
			owner.takeOffline();
		doException(e);
	}

	protected abstract void doConnect() throws SQLException;

	protected abstract void doDisconnect();

	protected abstract boolean doPing() throws SQLException;

	protected abstract void doReset() throws SQLException;

	protected abstract void doCommit() throws SQLException;

	protected abstract void doRollback() throws SQLException;

	// This function is called everytime an SQLException is encountered
	// by the database instance object. Implement it for database specific
	// exception handling.
	protected abstract void doException(SQLException e);

	// This function is supposed to return true when the database which
	// owns this instance should be taken offline, according to the
	// encountered exception.
	protected abstract boolean isCritical(SQLException e);
}
