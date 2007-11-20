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

package org.jsnap.server;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.Dbregistry;
import org.jsnap.exception.DefaultExceptionHandler;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.security.AccessDeniedException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.PasswordManagementException;
import org.jsnap.http.WebPageContainer;
import org.jsnap.http.pages.WebPage;
import org.jsnap.response.ResponseTracker;
import org.jsnap.security.Credentials;
import org.jsnap.security.AuthenticationPolicy.User;
import org.jsnap.util.JDocument;

public final class Workers extends ThreadPoolExecutor {
	private final static int DEFAULT_CORE_SIZE = 0;
	private final static int DEFAULT_MAXIMUM_SIZE = 1;
	private final static long DEFAULT_IDLE_TIMEOUT = 60; // seconds.

	public Workers(Dbregistry dbregistry, ResponseTracker responseTracker, WebPageContainer webPageContainer) {
		super(DEFAULT_CORE_SIZE, DEFAULT_MAXIMUM_SIZE, DEFAULT_IDLE_TIMEOUT, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		setThreadFactory(new WorkerFactory(dbregistry, responseTracker, webPageContainer));
		setRejectedExecutionHandler(new AbortPolicy());
	}

	public void setWebPageContainer() {
	}

	public void setCorePoolSize(int corePoolSize) {
		int current = getCorePoolSize();
		if (current != corePoolSize && corePoolSize >= 0) {
			Logger.getLogger(Workers.class).log(Level.DEBUG, "Core size of pool of workers is " + corePoolSize);
			super.setCorePoolSize(corePoolSize);
			prestartAllCoreThreads();
		}
	}

	public void setMaximumPoolSize(int maximumPoolSize) {
		int current = getMaximumPoolSize();
		if (current != maximumPoolSize && maximumPoolSize >= 0) {
			Logger.getLogger(Workers.class).log(Level.DEBUG, "Maximum size of pool of workers is " + maximumPoolSize);
			super.setMaximumPoolSize(maximumPoolSize);
		}
	}

	public void setKeepAliveTime(long time) {
		long current = getKeepAliveTime(TimeUnit.SECONDS);
		if (current != time && time >= 0) {
			Logger.getLogger(Workers.class).log(Level.DEBUG, "Idle timeout for worker threads is " + time + (time == 1 ? " second" : " seconds"));
			super.setKeepAliveTime(time, TimeUnit.SECONDS);
		}
	}

	private final class WorkerFactory implements ThreadFactory {
		private short counter = 0;
		private final Dbregistry dbregistry;
		private final ResponseTracker responseTracker;
		private final WebPageContainer webPageContainer;

		public WorkerFactory(Dbregistry dbregistry, ResponseTracker responseTracker, WebPageContainer webPageContainer) {
			this.dbregistry = dbregistry;
			this.responseTracker = responseTracker;
			this.webPageContainer = webPageContainer;
		}

	 	public Thread newThread(Runnable r) {
	 		++counter;
	 		if (counter == 1000)
		 		counter = 1;
	 		return new Worker("Worker-" + counter, r, dbregistry, responseTracker, webPageContainer);
		}
	}

	public final class Worker extends Thread {
		private final Dbregistry dbregistry;
		private final ResponseTracker responseTracker;
		private final WebPageContainer webPageContainer;

		public Worker(String name, Runnable r, Dbregistry dbregistry, ResponseTracker responseTracker, WebPageContainer webPageContainer) {
			super(r);
			setName(name);
			setDaemon(true);
			setUncaughtExceptionHandler(DefaultExceptionHandler.INSTANCE);
			this.dbregistry = dbregistry;
			this.responseTracker = responseTracker;
			this.webPageContainer = webPageContainer;
		}

		public void run() {
			Logger.getLogger(Workers.class).log(Level.INFO, "Up and running");
			super.run();
			Logger.getLogger(Workers.class).log(Level.INFO, "Terminated");
		}

		public DbInstance getDbInstance(String name) throws ConnectException {
			return dbregistry.get(name);
		}

		public DbInstance getDbInstance(String name, long timeout) throws ConnectException {
			return dbregistry.get(name, timeout);
		}

		public DbInstance getInternalDbInstance() throws ConnectException {
			return dbregistry.getInternalDb();
		}

		public DbInstance getInternalDbInstance(long timeout) throws ConnectException {
			return dbregistry.getInternalDb(timeout);
		}

		public User authenticate(String dbname, Credentials credentials) throws ConnectException, LoginFailedException {
			return dbregistry.authenticate(dbname, credentials);
		}

		public User authenticate(String dbname, Credentials credentials, long timeout) throws ConnectException, LoginFailedException {
			return dbregistry.authenticate(dbname, credentials, timeout);
		}

		public User authenticateWeb(Credentials credentials) throws ConnectException, LoginFailedException {
			return webPageContainer.authenticate(credentials);
		}

		public User authenticateWeb(Credentials credentials, long timeout) throws ConnectException, LoginFailedException {
			return webPageContainer.authenticate(credentials, timeout);
		}

		public void allowAccess(String dbname, Credentials credentials, String sql) throws ConnectException, AccessDeniedException {
			dbregistry.allowAccess(dbname, credentials, sql);
		}

		public void allowAccess(String dbname, Credentials credentials, String sql, long timeout) throws ConnectException, AccessDeniedException {
			dbregistry.allowAccess(dbname, credentials, sql, timeout);
		}

		public User changePassword(Credentials credentials, String password) throws ConnectException, PasswordManagementException {
			return dbregistry.changePassword(credentials, password);
		}

		public User changePassword(Credentials credentials, String password, long timeout) throws ConnectException, PasswordManagementException {
			return dbregistry.changePassword(credentials, password, timeout);
		}

		public ResponseTracker getResponseTracker() {
			return responseTracker;
		}

		public JDocument getPageList(boolean administrator) {
			JDocument doc = new JDocument("pages");
			if (webPageContainer != null) {
				WebPage[] pages = webPageContainer.getPageList(administrator);
				for (WebPage page: pages) {
					String[] names = new String[]{"category", "name", "key"};
					String[] data = new String[]{page.category(), page.name(), page.key()};
					doc.appendNodeHierarchy("item", names, data);
				}
			}
			return doc;
		}

		public WebPage getPage(String key) {
			return webPageContainer.getPage(key);
		}

		public WebPage getSummaryPage(boolean administrator) {
			return webPageContainer.getSummaryPage(administrator);
		}
	}
}
