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
import org.jsnap.db.base.DbResultSet;
import org.jsnap.exception.db.SqlException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.response.Formatter;
import org.jsnap.response.Response;
import org.jsnap.response.ResponseTracker.StoredResponse;
import org.jsnap.security.Credentials;
import org.jsnap.util.JDocument;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;

public abstract class AbstractWebMultiPage extends AbstractDbWebPage {
	public AbstractWebMultiPage(String documentRoot, String recordRoot) {
		xmlFormatterParameters = "metadata=false&resultset=" + documentRoot + "&record=" + recordRoot;
		this.documentRoot = documentRoot;
	}

	protected abstract WebResponse runOnce(HttpRequest request);
	protected abstract void initialize();
	protected abstract void appendPageData(JDocument doc);
	protected abstract boolean needsRefresh(HttpRequest request);
	protected abstract void refresh(HttpRequest request);
	protected abstract boolean operation(JDocument doc, HttpRequest request);
	protected abstract DbResultSet create(DbInstance dbi) throws SqlException;

	private static final int DEFAULT_PERPAGE = 20;
	private static final String PARAMETER_RUNONCE = "runonce";
	private static final String PARAMETER_PERPAGE = "perpage";
	private static final String PARAMETER_CURRENT = "current";
	private static final String PARAMETER_PAGES = "pages";
	private static final String PARAMETER_TOTAL = "total";
	protected static final String ERROR = "error";
	protected static final String AT = "at";

	public WebResponse data(HttpRequest request) {
		boolean runOnceFlag = (request.parameters.get(PARAMETER_RUNONCE) != null);
		if (runOnceFlag) {
			return runOnce(request);
		} else {
			// See if page's data needs to be refreshed.
			boolean needsRefresh = needsRefresh(request);
			if (sr == null || needsRefresh) {
				if (needsRefresh)
					refresh(request);
				clear(request);
			} else {
				current = JUtility.valueOf(request.parameters.get(PARAMETER_CURRENT), 1);
			}
			// Create page's data.
			JDocument doc = new JDocument(documentRoot);
			appendDefaults(doc);	// Default page data.
			initialize();			// Initilization, if required by the page.
			// Page specific operation, e.g. delete user, create group etc.
			if (operation(doc, request))
				clear(request);
			appendPageData(doc);	// Append page specific data.
			try {
				// Retrieves page specific information, e.g. user list, authentication attempts etc.
				JDocument dataDoc = dataDo(request.credentials);
				doc.copyFrom(dataDoc);
			} catch (SqlException e) {
				e.log();
				doc.appendTextNodeWithAttr(ERROR, e.getMessage(), new JPair<String, String>(AT, "data")); 
			}
			appendPageMetadata(doc);	// # of pages, current page etc.
			return new WebResponse(doc, stylesheet(), Formatter.XML, Formatter.DEFAULT);
		}
	}

	protected StoredResponse sr;
	private int perpage, current, pages, total;
	private final String documentRoot, xmlFormatterParameters;

	private DbResultSet createResultSet(DbInstance dbi) throws SqlException {
		DbResultSet result = create(dbi);
		try {
			total = 0;
			int records = 0;
			while (result.next()) {
				int counter = JUtility.intValueOf(result.get("counter"), 0);
				total += counter;
				++records;
			}
			pages = (records / perpage);
			if ((records % perpage) > 0)
				++pages;
			return result;
		} catch (SqlException e) {
			result.close();
			throw e;
		}
	}

	private void clear(HttpRequest request) {
		if (sr != null) {
			synchronized (sr) {
				sr.close();
			}
			sr = null;
		}
		perpage = JUtility.valueOf(request.parameters.get(PARAMETER_PERPAGE), DEFAULT_PERPAGE);
		current = 1;
		pages = 0;
		total = 0;
	}

	private void appendPageMetadata(JDocument doc) {
		doc.appendTextNode(PARAMETER_PERPAGE, Integer.toString(perpage));
		doc.appendTextNode(PARAMETER_CURRENT, Integer.toString(current));
		doc.appendTextNode(PARAMETER_PAGES, Integer.toString(pages));
		doc.appendTextNode(PARAMETER_TOTAL, Integer.toString(total));
	}

	private JDocument dataDo(Credentials credentials) throws SqlException {
		JDocument doc = null;
		while (doc == null) {
			if (sr == null) {
				Formatter f = Response.getXMLFormatter(xmlFormatterParameters);
				DbInstance dbi = getDbInstance();
				DbResultSet result;
				try {
					result = createResultSet(dbi);
				} catch (SqlException e) {
					dbi.close();
					throw e;
				}
				Response response = new Response(credentials, result, f);
				response.setResultSetVisibility(true);
				sr = createStoredResponse(response, dbi);
			}
			synchronized (sr) {
				if (sr.closed()) {
					sr = null; // Response has to be recreated.
				} else {
					if (current > pages)
						current = pages;
					sr.response.setMarks((current - 1) * perpage + 1, current * perpage);
					doc = (JDocument)sr.response.asObject();
					if (pages <= 1)	{	// Less than a page, response is displayed
						sr.close();		// all at once, no need to keep it stored.
						sr = null;
					}
				}
			}
		}
		return doc;
	}
}
