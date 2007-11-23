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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.Database;
import org.jsnap.db.base.DbHeader;
import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbInstanceTracker;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.exception.db.ResultSetNavigationException;
import org.jsnap.exception.db.ResultSetNotBoundException;
import org.jsnap.exception.db.ResultSetNotScrollableException;
import org.jsnap.exception.db.ResultSetReadException;
import org.jsnap.exception.db.SqlException;
import org.jsnap.exception.db.TimeoutException;
import org.jsnap.util.JPair;

public class Jdbc extends Database {
	public Jdbc(DbProperties prop, DbInstanceTracker instanceTracker) {
		super(prop, instanceTracker);
	}

	protected DbInstance getInstanceInternal() {
		return new JdbcInstance(this);
	}

	protected class JdbcInstance extends DbInstance {
		protected Connection conn;

		public JdbcInstance(Database owner) {
			super(owner);
		}

		protected DbStatement doCreateStatement(String sql, ArrayList<DbParam> parameters, boolean scrollable) throws SqlException {
			return new JdbcStatement(sql, parameters, scrollable);
		}

		protected void doConnect() throws SQLException {
			if (conn == null) {
				conn = DriverManager.getConnection(prop.url);
				conn.setAutoCommit(false);
			}
		}

		protected void doDisconnect() {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException ignore) {
					Logger.getLogger(JdbcInstance.class).log(Level.DEBUG, "Ignored a SQL exception during connection close", ignore);
				} finally {
					conn = null;
				}
			}
		}

		protected boolean doPing() throws SQLException {
			// Returns true if the latest connection attempt had succeeded.
			return (conn != null);
		}

		protected void doReset() throws SQLException {
			doRollback();
			conn.clearWarnings();
		}

		protected void doCommit() throws SQLException {
			conn.commit();
		}

		protected void doRollback() throws SQLException {
			conn.rollback();
		}

		protected void doException(SQLException e) {
			// No special handling for SQL exceptions. Hard to implement anything that is generic.
		}

		protected boolean isCritical(SQLException e) {
			// SQL exception is treated as a critical error only if there is no connection to the database.
			// That is, the exception is critical only if it is caught at connect time.
			return (conn == null);
		}		

		protected class JdbcStatement implements DbStatement {
			private final PreparedStatement stmt;

			public JdbcStatement(String sql, ArrayList<DbParam> parameters, boolean scrollable) throws SqlException {
				// Prepare statement.
				int rsType = (scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY);
				try {
					stmt = conn.prepareStatement(sql, rsType, ResultSet.CONCUR_READ_ONLY);
				} catch (SQLException e) {
					throw new SqlException(prop.name, e);
				}
				// Bind parameters.
				try {
					for (int i = 0; i < parameters.size(); ++i)
						bindParameter(i + 1, parameters.get(i));
				} catch (SQLException e) {
					try {
						stmt.close();
					} catch (SQLException ignore) {
						Logger.getLogger(JdbcStatement.class).log(Level.DEBUG, "Ignored a SQL exception during statement close", ignore);
					}
					throw new SqlException(prop.name, e);
				}
			}

			// Maps DbParam object to a JDBC type and its corresponding value object.
			public JPair<Integer, Object> typeMapping(DbParam param) throws SQLException {
				try {
					switch (param.type) {
					case DbParam.STRING:
						return new JPair<Integer, Object>(Types.VARCHAR, param.value);
					case DbParam.INTEGER:
						return new JPair<Integer, Object>(Types.INTEGER, Integer.valueOf(param.value));
					case DbParam.DOUBLE:
						return new JPair<Integer, Object>(Types.DOUBLE, Double.valueOf(param.value));
					case DbParam.TIMESTAMP:
						return new JPair<Integer, Object>(Types.TIMESTAMP, Timestamp.valueOf(param.value));
					case DbParam.LONG:
						return new JPair<Integer, Object>(Types.BIGINT, Long.valueOf(param.value));
					case DbParam.NUMERIC:
						// Fixed precision.
						return new JPair<Integer, Object>(Types.NUMERIC, new BigDecimal(param.value));
					case DbParam.FLOAT:
						// Types.REAL is single precision, Types.FLOAT is double precision.
						return new JPair<Integer, Object>(Types.REAL, Float.valueOf(param.value));
					case DbParam.SHORT:
						return new JPair<Integer, Object>(Types.SMALLINT, Short.valueOf(param.value));
					case DbParam.BYTE:
						return new JPair<Integer, Object>(Types.TINYINT, Byte.valueOf(param.value));
					case DbParam.BOOLEAN:
						return new JPair<Integer, Object>(Types.BOOLEAN, Boolean.valueOf(param.value));
					case DbParam.DATE:
						return new JPair<Integer, Object>(Types.DATE, Date.valueOf(param.value));
					case DbParam.TIME:
						return new JPair<Integer, Object>(Types.TIME, Time.valueOf(param.value));
					default:
						return new JPair<Integer, Object>(Types.OTHER, param.value);
					}
				} catch (Throwable t) {
					throw new SQLException(t);
				}
			}

			protected void bindParameter(int index, DbParam param) throws SQLException {
				JPair<Integer, Object> typeValuePair = typeMapping(param);
				// JPair.first denotes mapped JDBC type. JPair.second
				// holds the object that has the parameter's value.
				if (param.isNull)
					stmt.setNull(index, typeValuePair.first); 
				else
					stmt.setObject(index, typeValuePair.second, typeValuePair.first);
			}

			// Trust implementation of the JDBC driver for timeouts.
			// Unfortunately most JDBC drivers do not implement setQueryTimeout.
			public void setTimeout(long timeout) throws SqlException {
				// Timeout value comes here in a decreasing fashion through
				// the call stack and could be zero or negative at this point.
				if (timeout <= 0)
					throw new TimeoutException(prop.name);
				Logger.getLogger(JdbcStatement.class).log(Level.DEBUG, "Statement has a timeout: " + timeout + " millis");
				// JDBC's precision is at seconds level.
				int inSeconds = new Long(Math.round(timeout / 1000.0)).intValue();
				if (inSeconds == 0)
					inSeconds = 1; // Allow for one second of execution.
				try {
					stmt.setQueryTimeout(inSeconds);
				} catch (SQLException e) {
					try {
						stmt.close();
					} catch (SQLException ignore) {
						Logger.getLogger(JdbcStatement.class).log(Level.DEBUG, "Ignored a SQL exception during statement close", ignore);
					}
					throw new SqlException(prop.name, e);
				}
			}

			// Trust implementation of the JDBC driver for limiting row count.
			public void setMaxRows(int maxRows) throws SqlException {
				try {
					stmt.setMaxRows(maxRows);
				} catch (SQLException e) {
					try {
						stmt.close();
					} catch (SQLException ignore) {
						Logger.getLogger(JdbcStatement.class).log(Level.DEBUG, "Ignored a SQL exception during statement close", ignore);
					}
					throw new SqlException(prop.name, e);
				}
			}

			public DbResultSet execute() throws SqlException {
				boolean successful = true;
				try {
					boolean hasCursor = stmt.execute();
					if (hasCursor)
						return new JdbcResultSet(stmt.getResultSet());
					else
						return new JdbcResultSet(stmt);
				} catch (SQLException e) {
					successful = false;
					throw new SqlException(prop.name, e);
				} finally {
					if (successful == false) {
						try {
							stmt.close();
						} catch (SQLException ignore) {
							Logger.getLogger(JdbcStatement.class).log(Level.DEBUG, "Ignored a SQL exception during statement close", ignore);
						}
					}
				}
			}
		}
	}

	protected class JdbcResultSet implements DbResultSet {
		private final Statement generatedBy;
		private final DbHeader[] fields;
		private final int updateCount;
		private final ResultSet rs;
		private final boolean scrollable;
		private int pos, stop, rowCount;

		public JdbcResultSet(Statement generatedBy) throws SqlException {
			try {
				this.updateCount = generatedBy.getUpdateCount();
			} catch (SQLException e) {
				throw new SqlException(prop.name, e);
			}
			this.generatedBy = generatedBy;
			this.fields = new DbHeader[0];
			this.scrollable = false;
			this.rs = null;
			this.pos = -1;
			this.stop = -1;
			this.rowCount = -1;
		}
		
		public JdbcResultSet(ResultSet rs) throws SqlException {
			this.updateCount = 0;
			try {
				this.rs = rs;
				this.pos = 0;
				this.stop = 0;
				this.rowCount = -1;
				this.generatedBy = this.rs.getStatement();
				// See if result is scrollable.
				this.scrollable = (rs.getType() != ResultSet.TYPE_FORWARD_ONLY);
				// Create header information for the data stored in the result set.
				ResultSetMetaData md = rs.getMetaData();
				fields = new DbHeader[md.getColumnCount()];
				for (int col = 1; col <= fields.length; ++col) {
					int sqlType = md.getColumnType(col);
					Class javaType = typeMapping(sqlType);
					String name = md.getColumnName(col);
					int precision = md.getPrecision(col);
					int scale = md.getScale(col);
					fields[col - 1] = new DbHeader(javaType, name, precision, scale);
				}
			} catch (SQLException e) {
				throw new SqlException(prop.name, e);
			}
		}

		public String ownerdb() {
			return prop.name;
		}

		public boolean hasCursor() {
			return (rs != null);
		}

		public int affectedRows() {
			return updateCount;
		}

		public void limitBetween(int start, int stop) throws ResultSetException {
			positionAt(start);
			this.stop = stop;
		}

		public void positionAt(int position) throws ResultSetException {
			int to = (position < 1 ? 0 : position - 1);
			if (to < pos && scrollable == false)
				throw new ResultSetNotScrollableException(prop.name);
			int distToFirst = to;
			int distToLast = (scrollable == false || rowCount == -1 ? -1 : rowCount - to);
			int distToPos = Math.abs(pos - to);
			boolean rewind, forward;
			if (distToLast == -1) { // End of the result set has not been reached yet.
				rewind = (distToFirst < distToPos);
				forward = false;
			} else {
				rewind = (distToFirst <= distToLast && distToFirst < distToPos);
				forward = (distToLast <= distToFirst && distToLast < distToPos);
			}
			try {
				if (rewind) { // Rewind to the first record and go forward.
					rs.beforeFirst();
					pos = 0;
				} else if (forward) { // Go forward to the last record and return back.
					rs.last();
					pos = rowCount;
				}
				while (to != pos) {
					if (to > pos) {
						if (rs.next())
							++pos;
						else
							break;
					} else if (to < pos) {
						if (rs.previous())
							--pos;
						else
							break;
					}
				}
			} catch (SQLException e) {
				throw new ResultSetNavigationException(prop.name, e);
			}
			stop = 0;
		}

		public boolean next() throws ResultSetException {
			if (rs == null)
				throw new ResultSetNotBoundException(prop.name);
			try {
				if (stop == 0) {
					if (rs.next()) {
						++pos;
						return true;
					} else {
						rowCount = pos; // Reached end of the result set. Note down the record count.
						pos = rowCount + 1;
						return false;
					}
				} else {
					if (pos == stop) {
						return false;
					} else if (rs.next()) {
						++pos;
						return true;
					} else {
						rowCount = pos; // Reached end of the result set. Note down the record count.
						pos = rowCount + 1;
						return false;
					}
				}
			} catch (SQLException e) {
				throw new ResultSetNavigationException(prop.name, e);
			}
		}

		public int positionedAt() {
			return pos;
		}

		// Maps JDBC types to Java types.
		public Class typeMapping(int sqlType) {
			switch (sqlType) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				return String.class;
			case Types.INTEGER:
				return Integer.class;
			case Types.FLOAT:
			case Types.DOUBLE:
				return Double.class;
			case Types.TIMESTAMP:
				return Timestamp.class;
			case Types.BIGINT:
				return Long.class;
			case Types.DECIMAL:
			case Types.NUMERIC:
				return BigDecimal.class;
			case Types.REAL:
				return Float.class;
			case Types.SMALLINT:
				return Short.class;
			case Types.TINYINT:
				return Byte.class;
			case Types.BIT:
			case Types.BOOLEAN:
				return Boolean.class;
			case Types.DATE:
				return Date.class;
			case Types.TIME:
				return Time.class;
			default:
				return Object.class; // Generic.
			}
		}

		public int getColumnCount() {
			return fields.length;
		}

		public DbHeader whatis(int index) {
			return fields[index - 1];
		}

		public Object get(int index) throws ResultSetException {
			if (rs == null)
				throw new ResultSetNotBoundException(prop.name);
			if (index < 1 || index > fields.length)
				throw new ResultSetReadException(prop.name, "Index out of bounds [1-" + fields.length +"]");
			try {
				if (fields[index - 1].type.equals(String.class)) {
					return rs.getString(index);
				} else if (fields[index - 1].type.equals(Integer.class)) {
					return rs.getInt(index);
				} else if (fields[index - 1].type.equals(Double.class)) {
					return rs.getDouble(index);
				} else if (fields[index - 1].type.equals(Timestamp.class)) {
					return rs.getTimestamp(index);
				} else if (fields[index - 1].type.equals(Long.class)) {
					return rs.getLong(index);
				} else if (fields[index - 1].type.equals(BigDecimal.class)) {
					return rs.getBigDecimal(index);
				} else if (fields[index - 1].type.equals(Float.class)) {
					return rs.getFloat(index);
				} else if (fields[index - 1].type.equals(Short.class)) {
					return rs.getShort(index);
				} else if (fields[index - 1].type.equals(Byte.class)) {
					return rs.getByte(index);
				} else if (fields[index - 1].type.equals(Boolean.class)) {
					return rs.getBoolean(index);
				} else if (fields[index - 1].type.equals(Date.class)) {
					return rs.getDate(index);
				} else if (fields[index - 1].type.equals(Time.class)) {
					return rs.getTime(index);
				} else {
					return rs.getObject(index); // Generic.
				}
			} catch (SQLException e) {
				throw new ResultSetReadException(prop.name, e);
			}
		}

		public Object get(String fieldName) throws ResultSetException {
			if (rs == null)
				throw new ResultSetNotBoundException(prop.name);
			try {
				int index = rs.findColumn(fieldName);
				return get(index);
			} catch (SQLException e) {
				throw new ResultSetReadException(prop.name, "Could not locate a field named " + fieldName);
			}
		}

		public void close() {
			try {
				if (generatedBy != null)
					generatedBy.close();
			} catch (SQLException ignore) {
				Logger.getLogger(JdbcResultSet.class).log(Level.DEBUG, "Ignored a SQL exception during statement close", ignore);
			}
			try {
				if (rs != null)
					rs.close();
			} catch (SQLException ignore) {
				Logger.getLogger(JdbcResultSet.class).log(Level.DEBUG, "Ignored a SQL exception during result set close", ignore);
			}
		}
	}
}
