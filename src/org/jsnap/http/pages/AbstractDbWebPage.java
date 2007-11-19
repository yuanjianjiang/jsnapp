package org.jsnap.http.pages;

import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.db.SqlException;
import org.jsnap.http.base.HttpServlet;
import org.jsnap.response.Response;
import org.jsnap.response.ResponseTracker;
import org.jsnap.response.ResponseTracker.StoredResponse;
import org.jsnap.server.Workers.Worker;

public abstract class AbstractDbWebPage extends AbstractWebPage {
	private static final long RESULTSET_KEEPALIVE = 60000;

	private Worker executingThread;
	private ResponseTracker tracker;

	public AbstractDbWebPage() {
		executingThread = (Worker)Thread.currentThread();
		tracker = executingThread.getResponseTracker();
	}

	protected DbInstance getDbInstance() throws ConnectException {
		return executingThread.getInternalDbInstance(HttpServlet.WEB_DB_TIMEOUT);
	}

	protected StoredResponse createStoredResponse(Response response, DbInstance dbi) throws SqlException {
		return tracker.create(RESULTSET_KEEPALIVE, response, dbi);
	}
}
