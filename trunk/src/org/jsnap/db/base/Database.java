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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.Jdbc;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.db.DatabaseCreateException;
import org.jsnap.exception.db.NoDirectJDBCException;
import org.jsnap.exception.db.NoLongerUsableException;
import org.jsnap.exception.db.TimeoutException;
import org.jsnap.http.pages.WebPage;
import org.jsnap.security.AccessControlPolicy;
import org.jsnap.security.AuthenticationPolicy;
import org.jsnap.util.JUtility;

public abstract class Database {
	@SuppressWarnings("unchecked")
	public static Database create(DbProperties prop, DbInstanceTracker instanceTracker) throws DatabaseCreateException {
		if (prop.initial < 0)
			throw new DatabaseCreateException(prop.name, "initial cannot be negative");
		else if (prop.increment < 0)
			throw new DatabaseCreateException(prop.name, "increment cannot be negative");
		else if (prop.multiplier < 0)
			throw new DatabaseCreateException(prop.name, "multiplier cannot be negative");
		else if (prop.pool <= 0)
			throw new DatabaseCreateException(prop.name, "pool must be positive");
		else if (prop.idletimeout < 0)
			throw new DatabaseCreateException(prop.name, "idletimeout cannot be negative");
		else if (prop.login == null)
			throw new DatabaseCreateException(prop.name, "login cannot be null and must implement AuthenticationPolicy");
		else if (prop.accessControl == null)
			throw new DatabaseCreateException(prop.name, "accesscontrol cannot be null and must implement AccessControlPolicy");
		try {
			Class databaseClass = Class.forName(prop.driver);
			if (Database.class.isAssignableFrom(databaseClass)) {
				// JSnap specific handling.
				if (databaseClass.equals(Jdbc.class))
					throw new NoDirectJDBCException(prop.name);
				Constructor ctor = databaseClass.getConstructor(new Class[]{DbProperties.class, DbInstanceTracker.class});
				return (Database)ctor.newInstance(prop, instanceTracker);
			} else {
				// Generic JDBC.
				return new Jdbc(prop, instanceTracker);
			}
		} catch (ClassNotFoundException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (NoSuchMethodException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (SecurityException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (IllegalArgumentException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (InstantiationException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (IllegalAccessException e) {
			throw new DatabaseCreateException(prop.name, e);
		} catch (InvocationTargetException e) {
			throw new DatabaseCreateException(prop.name, e);
		}
	}

	// Use Database.create to instantiate a descendant of Database.
	protected Database(DbProperties prop, DbInstanceTracker instanceTracker) {
		this.instanceTracker = instanceTracker;
		this.creationTime = System.currentTimeMillis();
		this.nextAvailTime = this.creationTime;
		this.increment = prop.initial;
		this.prop = prop;
		this.pillow = new Object();
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true); // fair.
		this.write = lock.writeLock();
		this.read = lock.readLock();
		this.pool = new Stack<DbInstance>();
		for (int i = 0; i < prop.pool; ++i)
			this.pool.push(null); // Will be allocated later.
		this.usable = true;
		Logger logger = Logger.getLogger(this.getClass());
		logger.log(Level.INFO, "A new database has been created: " + toString());
		logger.log(Level.DEBUG, "url: <hidden> (" + this.prop.url.length() + " characters long)");
		logger.log(Level.DEBUG, "driver: " + this.prop.driver);
		logger.log(Level.DEBUG, "pool: " + this.prop.pool);
		logger.log(Level.DEBUG, "initial: " + this.prop.initial);
		logger.log(Level.DEBUG, "increment: " + this.prop.increment);
		logger.log(Level.DEBUG, "multiplier: " + this.prop.multiplier);
		logger.log(Level.DEBUG, "login: " + this.prop.login.getClass().getCanonicalName());
		logger.log(Level.DEBUG, "accesscontrol: " + this.prop.accessControl.getClass().getCanonicalName());
	}

	public boolean equals(Object o) {
		if (o != null && o.getClass().equals(this.getClass())) {
			Database d = (Database)o;
			read.lock();
			try {
				d.read.lock();
				try {
					return (prop.name.equals(d.prop.name) &&
							prop.url.equals(d.prop.url) &&
							prop.initial == d.prop.initial &&
							prop.increment == d.prop.increment &&
							prop.multiplier == d.prop.multiplier &&
							prop.pool == d.prop.pool &&
							prop.login.getClass().equals(d.prop.login.getClass()) &&
							prop.accessControl.getClass().equals(d.prop.accessControl.getClass()));
				} finally {
					d.read.unlock();
				}
			} finally {
				read.unlock();
			}
		}
		return false;
	}

	public String toString() {
		return prop.name + "(@" + creationTime + ")";
	}

	public DbStatus getStatus() {
		return new DbStatus();
	}

	protected boolean update(DbProperties prop) {
		write.lock();
		try {
			if (this.prop.name.equals(prop.name) && this.prop.url.equals(prop.url)) {
				this.prop.initial = prop.initial;
				this.prop.increment = prop.increment;
				this.prop.multiplier = prop.multiplier;
				synchronized (pillow) {
					if (this.prop.pool < prop.pool) {
						// Increase pool size.
						int add = prop.pool - this.prop.pool;
						for (int i = 0; i < add; ++i) {
							pool.push(null); // Will be allocated later.
							pillow.notify(); // Wake up, if any, a thread which is waiting for an instance.
						}
					} else if (this.prop.pool > prop.pool) {
						// Decrease pool size.
						int remove = this.prop.pool - prop.pool, removed = 0;
						for (int i = 0; i < pool.size() && removed < remove; ++i) {
							DbInstance dbi = pool.elementAt(i);
							if (dbi == null) { // Do not remove allocated instances at first pass.
								pool.remove(i);
								++removed;
							}
						}
						// Remove allocated instances only if necessary.
						while (pool.isEmpty() == false && removed < remove) {
							DbInstance dbi = pool.pop();
							dbi.disconnect(true);
							++removed;
						}
					}
					if (this.prop.idletimeout != prop.idletimeout) {
						long now = System.currentTimeMillis();
						for (int i = 0; i < pool.size(); ++i) {
							DbInstance dbi = pool.elementAt(i);
							if (dbi != null) {
								synchronized (dbi) {
									long lastActive = dbi.getLastActive();
									if (lastActive + prop.idletimeout < now)
										dbi.disconnect(true);
								}
								//long lastActive = dbi.getLastActive();
								//instanceTracker.disconnectIfIdleAt(dbi, lastActive + prop.idletimeout * 1000);
							}
						}
					}
				}
				this.prop.pool = prop.pool;
				this.prop.idletimeout = prop.idletimeout;
				this.prop.login = prop.login;
				this.prop.accessControl = prop.accessControl;
				return true;
			}
			return false;
		} finally {
			write.unlock();
		}
	}

	protected void doNotUseAnyLonger() {
		write.lock();
		try {
			usable = false;
			synchronized (pillow) {
				while (pool.isEmpty() == false) {
					DbInstance dbi = pool.pop();
					if (dbi != null)
						dbi.disconnect(true);
				}
			}
		} finally {
			write.unlock();
		}
	}

	protected void takeOnline() {
		write.lock();
		boolean wasOffline = off;
		try {
			nextAvailTime = System.currentTimeMillis();
			increment = prop.initial;
			off = false;
		} finally {
			write.unlock();
		}
		if (wasOffline)
			Logger.getLogger(this.getClass()).log(Level.INFO, prop.name + " is now online");
	}

	protected void takeOffline() {
		write.lock();
		long offlineFor = -1;
		try {
			if (offlineInternal() == false)
				offlineFor = takeOfflineInternal();
		} finally {
			write.unlock();
		}
		if (offlineFor > 0)
			Logger.getLogger(this.getClass()).log(Level.INFO, prop.name + " is now offline (for " + offlineFor + " seconds)");
	}

	public boolean offline() {
		read.lock();
		try {
			return offlineInternal();
		} finally {
			read.unlock();
		}
	}

	public WebPage[] getPages() {
		WebPage[] fromLogin = prop.login.getPages();
		WebPage[] fromAccessControl = prop.accessControl.getPages();
		WebPage[] all = new WebPage[fromLogin.length + fromAccessControl.length];
		int i = 0;
		for (WebPage page: fromLogin) {
			all[i] = page;
			++i;
		}
		for (WebPage page: fromAccessControl) {
			all[i] = page;
			++i;
		}
		return all;
	}

	public DbInstance getInstance() throws ConnectException {
		// Make sure that the database object is still usable.
		read.lock();
		try {
			if (usable == false)
				throw new NoLongerUsableException(prop.name);
		} finally {
			read.unlock();
		}
		// Obtain an instance.
		DbInstance dbi = null;
		boolean allocate = false;
		while (dbi == null) {
			boolean empty;
			allocate = false;
			synchronized (pillow) {
				empty = pool.isEmpty();
				if (empty == false)		// "dbi = pool.pop()", "dbi == null" and "pillow.wait()" must
					dbi = pool.pop();	// be atomic, so that no notify's are lost in between. That is
				if (dbi == null) {		// why this whole block is synchronized.
					if (empty) {
						try {
							pillow.wait();
						} catch (InterruptedException ignore) {
							Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
						}
					} else { 
						allocate = true;	// Do not block pillow with a getInstanceInternal().
					}
				}
			}
			if (allocate)					// Not yet initialized; recall that
				dbi = getInstanceInternal();// nulls are being pushed initially.
		}
		if (dbi != null)					// Make sure that the returned instance, if not
			prepareInstance(dbi, allocate); // null, is connected to the underlying database.
		return dbi;
	}

	public DbInstance getInstance(long timeout) throws ConnectException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(prop.name);
		long now = System.currentTimeMillis();
		long tryUntil = now + timeout;
		// Make sure that the database object is still usable.
		boolean acquired = false;
		while (acquired == false && (now < tryUntil)) {
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
				if (acquired) {
					try {
						if (usable == false)
							throw new NoLongerUsableException(prop.name);
					} finally {
						read.unlock();
					}
				}
			} catch (InterruptedException ignore) {
				Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
			}
			now = System.currentTimeMillis();
		}
		// Obtain an instance.
		DbInstance dbi = null;
		boolean allocate = false;
		while (dbi == null && now < tryUntil) {
			boolean empty;
			allocate = false;
			synchronized (pillow) {
				empty = pool.isEmpty();
				if (empty == false)		// "dbi = pool.pop()", "dbi == null" and "pillow.wait()" must
					dbi = pool.pop();	// be atomic, so that no notify's are lost in between. That is
				if (dbi == null) {		// why this whole block is synchronized.
					if (empty) {
						now = System.currentTimeMillis();
						try {
							pillow.wait(tryUntil - now);
						} catch (InterruptedException ignore) {
							Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
						}
					} else {
						allocate = true;	// Do not block pillow with a getInstanceInternal().
					}
				}
			}
			if (allocate)					// Not yet initialized; recall that
				dbi = getInstanceInternal();// nulls are being pushed initially.
			now = System.currentTimeMillis();
		}
		if (dbi != null)					// Make sure that the returned instance, if not
			prepareInstance(dbi, allocate); // null, is connected to the underlying database.
		return dbi;
	}

	protected AuthenticationPolicy getAuthenticationPolicy() throws NoLongerUsableException {
		read.lock();
		try {
			// Make sure that the database object is still usable.
			if (usable == false)
				throw new NoLongerUsableException(prop.name);
			return prop.login;
		} finally {
			read.unlock();
		}
	}

	protected AuthenticationPolicy getAuthenticationPolicy(long timeout) throws TimeoutException, NoLongerUsableException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(prop.name);
		long now = System.currentTimeMillis();
		long tryUntil = now + timeout;
		boolean acquired = false;
		while (acquired == false && (now < tryUntil)) {
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
				if (acquired) {
					try {
						// Make sure that the database object is still usable.
						if (usable == false)
							throw new NoLongerUsableException(prop.name);
						return prop.login;
					} finally {
						read.unlock();
					}
				}
			} catch (InterruptedException ignore) {
				Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
			}
			now = System.currentTimeMillis();
		}
		throw new TimeoutException(prop.name); // Timeout is certain if control gets here.
	}

	protected AccessControlPolicy getAccessControlPolicy() throws NoLongerUsableException {
		read.lock();
		try {
			// Make sure that the database object is still usable.
			if (usable == false)
				throw new NoLongerUsableException(prop.name);
			return prop.accessControl;
		} finally {
			read.unlock();
		}
	}

	protected AccessControlPolicy getAccessControlPolicy(long timeout) throws TimeoutException, NoLongerUsableException {
		// Timeout value comes here in a decreasing fashion through
		// the call stack and could be zero or negative at this point.
		if (timeout <= 0)
			throw new TimeoutException(prop.name);
		long now = System.currentTimeMillis();
		long tryUntil = now + timeout;
		boolean acquired = false;
		while (acquired == false && (now < tryUntil)) {
			try {
				acquired = read.tryLock(tryUntil - now, TimeUnit.MILLISECONDS);
				if (acquired) {
					try {
						// Make sure that the database object is still usable.
						if (usable == false)
							throw new NoLongerUsableException(prop.name);
						return prop.accessControl;
					} finally {
						read.unlock();
					}
				}
			} catch (InterruptedException ignore) {
				Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
			}
			now = System.currentTimeMillis();
		}
		throw new TimeoutException(prop.name); // Timeout is certain if control gets here.
	}

	protected void returnInstance(DbInstance dbi) {
		boolean disconnect = false;
		read.lock(); // For reading prop.poolSize.
		try {
			synchronized (pillow) {
				boolean push = (usable && (pool.size() + 1) <= prop.pool);
				if (push) {
					if (pool.isEmpty())
						pillow.notify();
					pool.push(dbi);
				} else {
					disconnect = true; // Does not block pillow with a forceDisonnect.
				}
			}
		} finally {
			read.unlock();
		}
		if (disconnect) {
			dbi.disconnect(true);
		} else {
			Logger.getLogger(this.getClass()).log(Level.DEBUG, "Connection to " + prop.name + " is returned to the pool");
		}
	}

	protected abstract DbInstance getInstanceInternal();

	private boolean offlineInternal() {
		return (nextAvailTime > System.currentTimeMillis());
	}

	private long takeOfflineInternal() {
		long oldIncrement = increment;
		nextAvailTime = System.currentTimeMillis() + (increment * 1000);
		increment = nextIncrement();
		off = true;
		return oldIncrement;
	}

	private long nextIncrement() {
		if (prop.multiplier != 0)
			return (long)(increment * prop.multiplier) + prop.increment;
		else if (prop.increment == 0)
			return prop.initial;
		else
			return increment + prop.increment;
	}

	private void prepareInstance(DbInstance dbi, boolean newlyAllocated) throws ConnectException {
		boolean reset = dbi.connect();
		Logger logger = Logger.getLogger(this.getClass());
		if (newlyAllocated) {
			logger.log(Level.DEBUG, "Opened a new connection to " + prop.name);
		} else {
			if (reset)
				logger.log(Level.DEBUG, "Database ping had failed, connection was reset");
			else
				logger.log(Level.DEBUG, "Acquired pooled connection to " + prop.name);
		}
	}

	protected void markInstanceAsIdle(DbInstance dbi) {
		if (prop.idletimeout > 0) { // Zero means no timeout.
			long timepoint = System.currentTimeMillis() + prop.idletimeout * 1000;
			instanceTracker.disconnectIfIdleAt(dbi, timepoint);
		}
	}

	public final DbProperties prop;
	private final DbInstanceTracker instanceTracker;
	private final Stack<DbInstance> pool; // Pool of available database instances.
	private final long creationTime;
	private final Lock read, write;
	private final Object pillow; // Used for synchronized access to the pool of database instances.
	private long nextAvailTime;
	private long increment;
	private boolean usable;
	private boolean off;

	public class DbStatus {
		public static final String ONLINE = "online";
		public static final String OFFLINE = "offline";
		public static final String UNUSABLE = "unusable";

		public final String name, status, whenAvailable;

		public DbStatus() {
			this.name = prop.name;
			if (usable) {
				if (offlineInternal()) {
					status = OFFLINE;
					whenAvailable = JUtility.toString(nextAvailTime);
				} else {
					status = ONLINE;
					whenAvailable = "";
				}
			} else {
				status = UNUSABLE;
				whenAvailable = "";
			}
		}
	}

	public static class DbProperties {
		public final String name, url;
		protected int pool;
		protected String driver;
		protected double multiplier;
		protected long initial, increment, idletimeout;
		private AuthenticationPolicy login;
		private AccessControlPolicy accessControl;

		public DbProperties(String name, String driver, String url, int pool, long idletimeout, long initial, long increment, double multiplier, AuthenticationPolicy login, AccessControlPolicy accessControl) {
			this.name = name;
			this.url = url;
			this.pool = pool;
			this.driver = driver;
			this.initial = initial;
			this.increment = increment;
			this.idletimeout = idletimeout;
			this.multiplier = multiplier;
			this.login = login;
			this.accessControl = accessControl;
		}

		public boolean equals(Object obj) {
			if (obj instanceof DbProperties) {
				DbProperties prop = (DbProperties)obj;
				return (name.equals(prop.name) &&
						url.equals(prop.url) &&
						pool == prop.pool &&
						driver.equals(prop.driver) &&
						initial == prop.initial &&
						increment == prop.increment &&
						idletimeout == prop.idletimeout &&
						multiplier == prop.multiplier &&
						login.getClass().equals(prop.login.getClass()) &&
						accessControl.getClass().equals(prop.accessControl.getClass()));
			}
			return false;
		}
	}
}
