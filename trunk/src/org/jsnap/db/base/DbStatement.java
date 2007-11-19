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
