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

package org.jsnap.http;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.Dbregistry;
import org.jsnap.db.base.Database.DbStatus;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.http.pages.AbstractWebPage;
import org.jsnap.http.pages.GroupManagement;
import org.jsnap.http.pages.UserManagement;
import org.jsnap.http.pages.WebPage;
import org.jsnap.http.pages.WebResponse;
import org.jsnap.response.Formatter;
import org.jsnap.response.ResponseTracker;
import org.jsnap.security.Credentials;
import org.jsnap.security.WebAuthenticate;
import org.jsnap.security.AuthenticationPolicy.User;
import org.jsnap.server.ListenerContainer;
import org.jsnap.server.Workers;
import org.jsnap.server.Listener.ListenerInfo;
import org.jsnap.util.JDocument;

public class WebPageContainer {
	// DEV: Produce pages about these internal structures.
	//private Workers workers;
	//private final ResponseTracker responseTracker;

	private final Dbregistry dbregistry;
	private final WebAuthenticate webAuthenticate;
	private final ListenerContainer listenerContainer;
	private final HashMap<String, WebPage> pageMap;

	public WebPageContainer(Dbregistry dbregistry, ResponseTracker responseTracker, ListenerContainer listenerContainer) {
		this.dbregistry = dbregistry;
		//this.responseTracker = responseTracker;
		this.listenerContainer = listenerContainer;
		this.webAuthenticate = new WebAuthenticate();
		//this.workers = null;
		this.pageMap = new HashMap<String, WebPage>();
	}

	public void setThreadPool(Workers workers) {
		//this.workers = workers;
	}

	public WebPage[] getPageList(boolean administrator) {
		WebPage[] pages;
		ArrayList<WebPage> pageList = new ArrayList<WebPage>();
		pages = dbregistry.getPages();
		for (WebPage page: pages) {
			if (administrator || page.administrative() == false)
				pageList.add(page);
		}
		pages = webAuthenticate.getPages();
		for (WebPage page: pages) {
			if (administrator || page.administrative() == false)
				pageList.add(page);
		}
		WebPage p = UserManagement.INSTANCE;
		if (administrator || p.administrative() == false)
			pageList.add(p);
		p = GroupManagement.INSTANCE;
		if (administrator || p.administrative() == false)
			pageList.add(p);
		// Transfer from ArrayList to a proper array.
		pages = new WebPage[pageList.size()];
		pageList.toArray(pages);
		pageMap.clear();
		for (WebPage page: pages)
			pageMap.put(page.key(), page);
		return pages;
	}

	public WebPage getSummaryPage(boolean administrator) {
		return new SummaryPage();
	}

	public WebPage getPage(String key) {
		return pageMap.get(key);
	}

	public User authenticate(Credentials credentials) throws ConnectException, LoginFailedException {
		DbInstance dbi = null;
		try {
			dbi = dbregistry.getInternalDb();
			return webAuthenticate.authenticate(dbi, credentials, Dbregistry.INTERNALDB_NAME);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public User authenticate(Credentials credentials, long timeout) throws ConnectException, LoginFailedException {
		long tryUntil = System.currentTimeMillis() + timeout;
		DbInstance dbi = null;
		try {
			dbi = dbregistry.getInternalDb(timeout);
			long remaining = tryUntil - System.currentTimeMillis();
			return webAuthenticate.authenticate(dbi, credentials, Dbregistry.INTERNALDB_NAME, remaining);
		} finally {
			if (dbi != null)
				dbi.close();
		}
	}

	public final class SummaryPage extends AbstractWebPage {
		public String key() {
			return "info-summary";
		}

		public String name() {
			return "Summary Page";
		}

		public String category() {
			return WebPage.CATEGORY_INFORMATION;
		}

		public String stylesheet() {
			return "/jwc/summary.xsl";
		}

		public boolean administrative() {
			return false;
		}

		public WebResponse data(HttpRequest request) {
			JDocument doc = new JDocument("summary");
			appendDefaults(doc);
			DbStatus[] databases = dbregistry.getList();
			for (DbStatus db: databases) {
				String[] names = new String[]{"name", "status", "whenAvailable"};
				doc.appendNodeHierarchy("database", names , new String[]{db.name, db.status, db.whenAvailable});
			}
			ListenerInfo[] listeners = listenerContainer.info();
			for (ListenerInfo listener: listeners) {
				String[] names = new String[]{"port", "accepts"};
				doc.appendNodeHierarchy("listener", names, new String[]{Integer.toString(listener.listeningTo), listener.accepts});
			}
			return new WebResponse(doc, stylesheet(), Formatter.XML, Formatter.DEFAULT);
		}
	}
}
