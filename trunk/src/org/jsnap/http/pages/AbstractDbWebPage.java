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
