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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.Database.DbProperties;
import org.jsnap.db.base.Database.DbStatus;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.db.DatabaseCreateException;
import org.jsnap.exception.db.InternalDbUnavailable;
import org.jsnap.exception.db.SqlException;
import org.jsnap.exception.db.TimeoutException;
import org.jsnap.exception.db.UnknownNameException;
import org.jsnap.exception.security.AccessDeniedException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.PasswordManagementException;
import org.jsnap.http.pages.WebPage;
import org.jsnap.security.AccessControlPolicy;
import org.jsnap.security.Authenticate;
import org.jsnap.security.AuthenticationPolicy;
import org.jsnap.security.Credentials;
import org.jsnap.security.AuthenticationPolicy.User;

public final class Dbregistry {
	// DEV: Web interface to clear and backup internal database.
	public final static String INTERNALDB_NAME = "internaldb";

	public Dbregistry() {
		internal = null;
		registry = new HashMap<String, Database>();
		instanceTracker = new DbInstanceTracker();
		ReadWriteLock lock = new ReentrantReadWriteLock(true); // fair.
		read = lock.readLock();
		write = lock.writeLock();
	}

	public DbStatus[] getList() {
		read.lock();
		try {
			int i;
			DbStatus[] list;
			if (internal != null) {
				list = new DbStatus[registry.size() + 1];
				list[0] = internal.getStatus();
				i = 1;
			} else {
				list = new DbStatus[registry.size()];
				i = 0;
			}
			for (String name: registry.keySet()) {
				Database db = registry.get(name);
				list[i] = db.getStatus();
				++i;
			}
			return list;
		} finally {
			read.unlock();
		}
	}

	public WebPage[] getPages() {
		WebPage[] pages;
		ArrayList<WebPage> pageList = new ArrayList<WebPage>();
		read.lock();
		try {
			if (internal != null) {
				pages = internal.getPages();
				for (WebPage page: pages)
					pageList.add(page);
			}
			for (String name: registry.keySet()) {
				Database db = registry.get(name);
				pages = db.getPages();
				for (WebPage page: pages)
					pageList.add(page);
			}
		} finally {
			read.unlock();
		}
		pages = new WebPage[pageList.size()];
		pageList.toArray(pages);
		return pages;
	}

	public DbInstance get(String name) throws ConnectException {
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Connecting to " + name);
		DbInstance dbi = null;
		while (dbi == null) {
			read.lock();
			Database db;
			try {
				if (name.equals(INTERNALDB_NAME)) {
					db = internal;
					if (db == null)
						throw new InternalDbUnavailable();
				} else {
					db = registry.get(name);
					if (db == null)
						throw new UnknownNameException(name);
				}
			} finally {
				read.unlock();
			}
			dbi = db.getInstance();
		}
		return dbi;
	}

	public DbInstance get(String name, long timeout) throws ConnectException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(name);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Connecting to " + name);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Attempt has a timeout: " + timeout + " millis");
		DbInstance dbi = null;
		long now = System.currentTimeMillis(), tryUntil = now + timeout;
		while (dbi == null && (timeout == 0 || now < tryUntil)) {
			boolean acquired;
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				acquired = false;
			}
			if (acquired) {
				Database db;
				try {
					if (name.equals(INTERNALDB_NAME)) {
						db = internal;
						if (db == null)
							throw new InternalDbUnavailable();
					} else {
						db = registry.get(name);
						if (db == null)
							throw new UnknownNameException(name);
					}
				} finally {
					read.unlock();
				}
				now = System.currentTimeMillis();
				dbi = db.getInstance(tryUntil - now);
			}
			now = System.currentTimeMillis();
		}
		if (dbi == null)
			throw new TimeoutException(name);
		return dbi;
	}

	public DbInstance getInternalDb() throws ConnectException {
		// Do this only here because this call has no timeout!
		if (internal != null) {
			synchronized (internal) {
				if (internalChecked == false)
					internalChecked = check(internal);
			}
		}
		return get(INTERNALDB_NAME);
	}

	public DbInstance getInternalDb(long timeout) throws ConnectException {
		return get(INTERNALDB_NAME, timeout);
	}

	public User authenticate(String dbname, Credentials credentials) throws ConnectException, LoginFailedException {
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Authenticating " + credentials.username + " at " + dbname);
		read.lock();
		Database db = null;
		try {
			if (dbname.equals(INTERNALDB_NAME)) {
				db = internal;
				if (db == null)
					throw new InternalDbUnavailable();
			} else {
				db = registry.get(dbname);
				if (db == null)
					throw new UnknownNameException(dbname);
			}
		} finally {
			read.unlock();
		}
		AuthenticationPolicy policy = db.getAuthenticationPolicy();
		DbInstance dbi = null;
		try {
			dbi = getInternalDb();
			return policy.authenticate(dbi, credentials, dbname);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public User authenticate(String dbname, Credentials credentials, long timeout) throws ConnectException, LoginFailedException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(dbname);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Authenticating " + credentials.username + " at " + dbname);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Attempt has a timeout: " + timeout + " millis");
		Database db = null;
		long now = System.currentTimeMillis(), tryUntil = now + timeout;
		while (db == null && (timeout == 0 || now < tryUntil)) {
			boolean acquired;
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				acquired = false;
			}
			if (acquired) {
				try {
					if (dbname.equals(INTERNALDB_NAME)) {
						db = internal;
						if (db == null)
							throw new InternalDbUnavailable();
					} else {
						db = registry.get(dbname);
						if (db == null)
							throw new UnknownNameException(dbname);
					}
				} finally {
					read.unlock();
				}
			}
			now = System.currentTimeMillis();
		}
		if (db == null)
			throw new TimeoutException(dbname);
		now = System.currentTimeMillis();
		AuthenticationPolicy policy = db.getAuthenticationPolicy(tryUntil - now);
		DbInstance dbi = null;
		try {
			now = System.currentTimeMillis();
			dbi = getInternalDb(tryUntil - now);
			now = System.currentTimeMillis();
			return policy.authenticate(dbi, credentials, dbname, tryUntil - now);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public void allowAccess(String dbname, Credentials credentials, String sql) throws ConnectException, AccessDeniedException {
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Checking access to " + dbname + " by " + credentials.username);
		read.lock();
		Database db;
		try {
			if (dbname.equals(INTERNALDB_NAME)) {
				db = internal;
				if (db == null)
					throw new InternalDbUnavailable();
			} else {
				db = registry.get(dbname);
				if (db == null)
					throw new UnknownNameException(dbname);
			}
		} finally {
			read.unlock();
		}
		AccessControlPolicy policy = db.getAccessControlPolicy();
		DbInstance dbi = null;
		try {
			dbi = getInternalDb();
			policy.allowAccess(dbi, credentials, dbname, sql);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public void allowAccess(String dbname, Credentials credentials, String sql, long timeout) throws ConnectException, AccessDeniedException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(dbname);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Checking access to " + dbname + " by " + credentials.username);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Attempt has a timeout: " + timeout + " millis");
		Database db = null;
		long now = System.currentTimeMillis(), tryUntil = now + timeout;
		while (db == null && (timeout == 0 || now < tryUntil)) {
			boolean acquired;
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				acquired = false;
			}
			if (acquired) {
				try {
					if (dbname.equals(INTERNALDB_NAME)) {
						db = internal;
						if (db == null)
							throw new InternalDbUnavailable();
					} else {
						db = registry.get(dbname);
						if (db == null)
							throw new UnknownNameException(dbname);
					}
				} finally {
					read.unlock();
				}
			}
			now = System.currentTimeMillis();
		}
		if (db == null)
			throw new TimeoutException(dbname);
		now = System.currentTimeMillis();
		AccessControlPolicy policy = db.getAccessControlPolicy(tryUntil - now);
		DbInstance dbi = null;
		try {
			now = System.currentTimeMillis();
			dbi = getInternalDb(tryUntil - now);
			now = System.currentTimeMillis();
			policy.allowAccess(dbi, credentials, dbname, sql, tryUntil - now);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public User changePassword(Credentials credentials, String password) throws ConnectException, PasswordManagementException {
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Changing " + credentials.username + "'s password");
		read.lock();
		Database db;
		try {
			db = internal;
			if (db == null)
				throw new InternalDbUnavailable();
		} finally {
			read.unlock();
		}
		// We know that internal database has an AuthenticationPolicy of WebAuthenticate, descendant of Authenticate.
		Authenticate policy = (Authenticate)db.getAuthenticationPolicy();
		DbInstance dbi = null;
		try {
			dbi = getInternalDb();
			return policy.changePassword(dbi, credentials, password);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public User changePassword(Credentials credentials, String password, long timeout) throws ConnectException, PasswordManagementException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(INTERNALDB_NAME);
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Changing " + credentials.username + "'s password");
		Logger.getLogger(Dbregistry.class).log(Level.DEBUG, "Attempt has a timeout: " + timeout + " millis");
		Database db = null;
		long now = System.currentTimeMillis(), tryUntil = now + timeout;
		while (db == null && (timeout == 0 || now < tryUntil)) {
			boolean acquired;
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				acquired = false;
			}
			if (acquired) {
				try {
					db = internal;
					if (db == null)
						throw new InternalDbUnavailable();
				} finally {
					read.unlock();
				}
			}
			now = System.currentTimeMillis();
		}
		if (db == null)
			throw new TimeoutException(INTERNALDB_NAME);
		now = System.currentTimeMillis();
		// We know that internal database has an AuthenticationPolicy of WebAuthenticate, descendant of Authenticate.
		Authenticate policy = (Authenticate)db.getAuthenticationPolicy(tryUntil - now);
		DbInstance dbi = null;
		try {
			now = System.currentTimeMillis();
			dbi = getInternalDb(tryUntil - now);
			now = System.currentTimeMillis();
			return policy.changePassword(dbi, credentials, password, tryUntil - now);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public void update(DbProperties[] databases) {
		write.lock();
		// Backup the current registry as the old one and fill a newly created registry.
		HashMap<String, Database> oldRegistry = registry;
		registry = new HashMap<String, Database>();
		try {
			for (DbProperties prop: databases) {
				Database existing = oldRegistry.get(prop.name);
				// Database is new to the registry, it did not exist previously. Just put it in:
				if (existing == null) {
					try {
						Database db = Database.create(prop, instanceTracker);
						registry.put(db.prop.name, db);
						Logger.getLogger(Dbregistry.class).log(Level.INFO, db + " has been registered");
					} catch (DatabaseCreateException e) {
						e.log(); // logged and skipped.
					}
				// A database with the same name existed previously:
				} else {
					// Properties of the new and existing databases are not equal:
					if (prop.equals(existing.prop) == false) {
						boolean updated = existing.update(prop);
						// Existing database was updated successfully, it could be
						// put into the registry rather than creating a new one:
						if (updated) {
							registry.put(existing.prop.name, existing);
							Logger.getLogger(Dbregistry.class).log(Level.INFO, existing + " has been updated");
						// Existing database could not be updated; it will be discarded and
						// a newly created database will be put in the registry:
						} else {
							existing.doNotUseAnyLonger();
							Logger.getLogger(Dbregistry.class).log(Level.INFO, existing + " has been unregistered");
							try {
								Database db = Database.create(prop, instanceTracker);
								registry.put(db.prop.name, db);
								Logger.getLogger(Dbregistry.class).log(Level.INFO, db + " has been registered");
							} catch (DatabaseCreateException e) {
								e.log(); // logged and skipped.
							}
						}
					// Properties of the new and existing databases are equal. Put
					// existing database into the registry, no need to create a new one:
					} else {
						registry.put(existing.prop.name, existing);
					}
					oldRegistry.remove(prop.name); // This removal is necessary so that oldRegistry is left
				}								   // only with the databases that need to be discarded.
			}
		} finally {
			write.unlock();
		}
		// Discard the databases which existed in the old registry but
		// does not exist in the new list that is passed as a parameter.
		for (String name: oldRegistry.keySet()) {
			Database db = oldRegistry.get(name);
			db.doNotUseAnyLonger();
			Logger.getLogger(Dbregistry.class).log(Level.INFO, db + " has been unregistered");
		}
	}

	public void update(DbProperties prop) {
		write.lock();
		try {
			if (internal == null || internal.prop.equals(prop) == false) {
				try {
					Database m = Database.create(prop, instanceTracker);
					internalChecked = check(m); // Check if all necessary objects are there.
					if (internal != null) {
						internal.doNotUseAnyLonger();
						Logger.getLogger(Dbregistry.class).log(Level.INFO, "Old internal database " + internal + " is discarded");
					}
					internal = m;
				} catch (DatabaseCreateException e) {
					e.log(); // logged and skipped.
				}
				if (internal == null)
					Logger.getLogger(Dbregistry.class).log(Level.WARN, "No internal database is currently in use");
				else
					Logger.getLogger(Dbregistry.class).log(Level.INFO, "Using " + internal + " as internal database");
			}
		} finally {
			write.unlock();
		}
	}

	public void shutdown() {
		update(new DbProperties[]{});
		write.lock();
		try {
			if (internal != null) {
				internal.doNotUseAnyLonger();
				internal = null;
				Logger.getLogger(Dbregistry.class).log(Level.INFO, "Internal database has been shutdown");
			}
		} finally {
			write.unlock();
		}
	}

	private Database internal;
	private boolean internalChecked;
	private HashMap<String, Database> registry;
	private final DbInstanceTracker instanceTracker;
	// write lock is acquired when an entry in the registry or the whole registry is being modified.
	// read lock is acquired when the registry is read.
	private final Lock read, write;

	private static boolean check(Database db) {
		DbStatement statement;
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		try {
			DbInstance dbi = db.getInstance();
			try {
				Logger.getLogger(Dbregistry.class).log(Level.INFO, "Checking internal database");
				parameters.clear();
				parameters.add(new DbParam(DbParam.INTEGER, "0", false));
				statement = dbi.createStatement(CHECK_TABLE_USERS, parameters, false);
				statement.execute().close();
				parameters.clear();
				parameters.add(new DbParam(DbParam.INTEGER, "0", false));
				statement = dbi.createStatement(CHECK_TABLE_GROUPS, parameters, false);
				statement.execute().close();
				parameters.clear();
				parameters.add(new DbParam(DbParam.INTEGER, "0", false));
				parameters.add(new DbParam(DbParam.INTEGER, "0", false));
				statement = dbi.createStatement(CHECK_TABLE_MEMBERSHIPS, parameters, false);
				statement.execute().close();
				parameters.clear();
				parameters.add(new DbParam(DbParam.STRING, "dummy", false));
				statement = dbi.createStatement(CHECK_TABLE_PARAMETERS, parameters, false);
				statement.execute().close();
				parameters.clear();
				parameters.add(new DbParam(DbParam.STRING, "dummy", false));
				statement = dbi.createStatement(CHECK_TABLE_AUTHHISTORY, parameters, false);
				statement.execute().close();
				parameters.clear();
				statement = dbi.createStatement(CHECK_FUNCTION_TIMESTAMP_TO_STRING, parameters, false);
				statement.execute().close();
				statement = dbi.createStatement(CHECK_FUNCTION_TIMESTAMP_TO_SECONDS, parameters, false);
				statement.execute().close();
				statement = dbi.createStatement(CHECK_FUNCTION_STRING_TO_SECONDS, parameters, false);
				statement.execute().close();
				statement = dbi.createStatement(CHECK_FUNCTION_REASON_TO_STRING, parameters, false);
				statement.execute().close();
				Logger.getLogger(Dbregistry.class).log(Level.INFO, "Check complete");
			} catch (SqlException e) {
				e.log(); // logged for information.
				Logger.getLogger(Dbregistry.class).log(Level.WARN, "Check on internal database failed");
			} finally {
				dbi.close();
			}
		} catch (ConnectException e) {
			e.log();
			return false; // ConnectException is sort of tolerated, however, check is not yet complete.
		}
		return true; // Check is complete. 
	}

	private final static String CHECK_TABLE_USERS = "SELECT userid, username, password, " +
	   													   "mdalg, admin, actlocked, pwdexpired " +
	   												  "FROM users WHERE userid = ?";
	private final static String CHECK_TABLE_GROUPS = "SELECT groupid, groupname, administrative FROM groups WHERE groupid = ?";
	private final static String CHECK_TABLE_MEMBERSHIPS = "SELECT groupid, userid FROM memberships WHERE groupid = ? AND userid = ?";
	private final static String CHECK_TABLE_PARAMETERS = "SELECT name, value " +
	   													   "FROM parameters WHERE name = ?";
	private final static String CHECK_TABLE_AUTHHISTORY = "SELECT username, type, result, timestamp, ipaddress " +
														   "FROM authhistory WHERE username = ?";
	private final static String CHECK_FUNCTION_TIMESTAMP_TO_STRING = "SELECT TIMESTAMP_TO_STRING(CURRENT_TIMESTAMP, 'minute') AS dummy FROM dual";
	/*
	org.jsnap.exception.db.SqlException: JSNAP-02000(@internaldb): Native SQL exception
	        at org.jsnap.db.Jdbc$JdbcInstance$JdbcStatement.<init>(Jdbc.java:111)
	        at org.jsnap.db.Jdbc$JdbcInstance.doCreateStatement(Jdbc.java:52)
	        at org.jsnap.db.base.DbInstance.createStatement(DbInstance.java:94)
	        at org.jsnap.db.base.Dbregistry.check(Dbregistry.java:528)
	        at org.jsnap.db.base.Dbregistry.update(Dbregistry.java:458)
	        at org.jsnap.ConfigurationWatchdog.trace(ConfigurationWatchdog.java:184)
	        at org.jsnap.ConfigurationWatchdog.trace(ConfigurationWatchdog.java:253)
	        at org.jsnap.ConfigurationWatchdog.configure(ConfigurationWatchdog.java:147)
	        at org.jsnap.ConfigurationWatchdog.run(ConfigurationWatchdog.java:86)
	        at java.lang.Thread.run(Thread.java:595)
	Caused by: java.sql.SQLException: S1000 General error java.lang.ArrayIndexOutOfBoundsException: 1 in statement [SELECT TIMESTAMP_TO_STRING(CURRENT_TIMESTAMP, 'minute') AS dummy FROM dual]
	        at org.hsqldb.jdbc.Util.throwError(Unknown Source)
	        at org.hsqldb.jdbc.jdbcPreparedStatement.<init>(Unknown Source)
	        at org.hsqldb.jdbc.jdbcConnection.prepareStatement(Unknown Source)
	        at org.jsnap.db.Jdbc$JdbcInstance$JdbcStatement.<init>(Jdbc.java:109)
	        ... 9 more
	*/
	private final static String CHECK_FUNCTION_TIMESTAMP_TO_SECONDS = "SELECT TIMESTAMP_TO_SECONDS(CURRENT_TIMESTAMP) AS dummy FROM dual";
	private final static String CHECK_FUNCTION_STRING_TO_SECONDS = "SELECT STRING_TO_SECONDS('01/01/1970 00:00:00') AS dummy FROM dual";
	private final static String CHECK_FUNCTION_REASON_TO_STRING = "SELECT REASON_TO_STRING(0) AS dummy FROM dual";
}
