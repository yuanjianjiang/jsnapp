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

package org.jsnap.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jsnap.db.base.DbResultSet;
import org.jsnap.exception.comm.MalformedResponseException;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.util.JDocument;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;
import org.xml.sax.SAXException;

public final class HTMLFormatter implements Formatter {
	private static final int DEFAULT_PERPAGE = 20;
	private static final String HKEY = KEY.toLowerCase();

	private String css = null, home = null;
	private int perpage = DEFAULT_PERPAGE;

	public void setParameter(String name, String value) {
		if (name.equals("css")) {
			css = value;
		} else if (name.equals("home")) {
			home = value;
		} else if (name.equals("perpage")) {
			perpage = JUtility.valueOf(value, DEFAULT_PERPAGE);
			if (perpage <= 0)
				perpage = DEFAULT_PERPAGE;
		}
	}

	public byte[] format(Response response) throws ResultSetException {
		return formatAsObject(response).getBytes();
	}

	public String formatAsObject(Response response) throws ResultSetException {
		String body;
		int key = response.getKey();
		DbResultSet result = response.getResult();
		JPair<Boolean, Boolean> state = response.getOwnerState();
		if (state == null || state.first == true) {
			if (result == null) {
				body = skipInitial(response, key);
			} else {
				if (result.hasCursor())
					body = showCursor(response, result, key);
				else
					body = showAffected(response, result, key);
			}
		} else {
			if (result == null)
				body = showDone();
			else
				body = showState(state.second, result.affectedRows());
		}
		return body;
	}

	public int extractKey(byte[] response) throws MalformedResponseException {
		try {
			JDocument doc = JDocument.readFrom(new ByteArrayInputStream(response));
			String key = doc.getFirstTextContent(HKEY);
			if (key == null)
				throw new MalformedResponseException("No key is specified in this response");
			int k = JUtility.valueOf(key, -1);
			if (k <= 0)
				throw new MalformedResponseException("Key value is not positive or non-number");
			return k;
		} catch (SAXException e) {
			throw new MalformedResponseException("XML parser failed: " + e.getMessage());
		} catch (IOException e) {
			throw new MalformedResponseException("I/O exception while parsing: " + e.getMessage());
		}
	}

	private String skipInitial(Response response, int key) throws ResultSetException {
		String body = "<html>\r\n";
		body += "  <" + HKEY + ">" + key + "</" + HKEY + ">\r\n";
		body += "  <body onload=\"document.access.submit();\">\r\n";
		body += "  <form name=\"access\" action=\"/execute\" method=\"post\">\r\n";
		body += "  <input type=\"hidden\" name=\"command\" value=\"access\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"key\" value=\"" + key + "\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"timeout\" value=\"0\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"zip\" value=\"1\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"from\" value=\"1\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"to\" value=\"" + perpage + "\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"username\" value=\"" + response.credentials.username + "\"/>\r\n";
		body += "  <input type=\"hidden\" name=\"password\" value=\"" + response.credentials.password + "\"/>\r\n";
		body += "  </form>\r\n";
		body += "  </body>\r\n";
		body += "</html>\r\n";
		return body;
	}

	private String showCursor(Response response, DbResultSet result, int key) throws ResultSetException {
		String body = "<html>\r\n";
		body += "  <head>\r\n";
		body += "    <title>SQL Output</title>\r\n";
		if (css != null)
			body += "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + css + "\"/>\r\n";
		body += "  </head>\r\n";
		body += "  <body>\r\n";
		boolean multipage = (key > 0);
		if (multipage)
			body += "  <form name=\"access\" action=\"/execute\" method=\"post\">\r\n";
		body += "  <table class=\"main\">\r\n";
		body += "  <tr class=\"header\">\r\n";
		int columnCount = result.getColumnCount();
		for (int i = 1; i <= columnCount; ++i)
			body += "  <td class=\"header\">" + result.whatis(i).name + "</td>\r\n";
		body += "  </tr>\r\n";
		int from = result.positionedAt() + 1;
		int rowCount = 0;
		boolean gray = false;
		while (result.next()) {
			body += "  <tr class=\"" + (gray ? "gray" : "white")+ "\">\r\n";
			for (int i = 1; i <= columnCount; ++i)
				body += "  <td class=\"" + (gray ? "gray" : "white")+ "\">" + result.get(i).toString() + "</td>\r\n";
			body += "  </tr>\r\n";
			gray = !gray;
			++rowCount;
		}
		int to = from + rowCount - 1;
		if (multipage && rowCount == 0)
			body += "  <tr><td>End of result set</td></tr>";
		body += "  </table>\r\n";
		if (multipage) {
			body += "  <table class=\"footer\" width=\"100%\">\r\n";
			body += "  <tr>\r\n";
			if (from > 1) {
				int f = (from > perpage ? from - perpage : 1);
				int t = f + perpage - 1;
				body += "  <td width=\"33%\" align=\"left\"><a href=\"#\" onclick=\"document.access.from.value='" + Integer.toString(f) +
						"';document.access.to.value='" + Integer.toString(t) + "';document.access.submit();return false;\">Previous Page</a></td>\r\n";
			} else {
				body += "  <td width=\"33%\"></td>\r\n";
			}
			if (rowCount > 0)
				body += "  <td width=\"34%\" align=\"center\">Displaying " + from + "-" + to + "</td>\r\n";
			else
				body += "  <td width=\"34%\"></td>\r\n";
			if (rowCount == perpage) {
				int f = from + perpage;
				int t = f + perpage - 1;
				body += "  <td width=\"33%\" align=\"right\"><a href=\"#\" onclick=\"document.access.from.value='" + Integer.toString(f) +
						"';document.access.to.value='" + Integer.toString(t) + "';document.access.submit();return false;\">Next Page</a></td>\r\n";
			} else {
				body += "  <td width=\"33%\"></td>\r\n";
			}
			body += "  </tr>\r\n";
			body += "  </table>\r\n";
			body += "  <p align=\"center\"><a href=\"#\" onclick=\"document.access.command.value='commit';document.access.submit();return false;\">Done</a></p>\r\n";
			body += "  <input type=\"hidden\" name=\"command\" value=\"access\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"key\" value=\"" + key + "\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"timeout\" value=\"0\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"zip\" value=\"1\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"from\" value=\"-1\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"to\" value=\"-1\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"username\" value=\"" + response.credentials.username + "\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"password\" value=\"" + response.credentials.password + "\"/>\r\n";
			body += "  </form>\r\n";
		} else {
			if (home != null)
				body += "  <p><a href=\"" + home + "\">Home</a></p>\r\n";
		}
		body += "  </body>\r\n";
		body += "</html>\r\n";
		return body;
	}

	private String showAffected(Response response, DbResultSet result, int key) throws ResultSetException {
		String body = "<html>\r\n";
		body += "  <head>\r\n";
		body += "    <title>SQL Output</title>\r\n";
		if (css != null)
			body += "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + css + "\"/>\r\n";
		body += "  </head>\r\n";
		body += "  <body>\r\n";
		boolean awaiting = (key > 0);
		body += "  <p>" + AFFECTED + ": " + result.affectedRows() + "</p>";
		if (awaiting) {
			body += "  <form name=\"access\" action=\"/execute\" method=\"post\">\r\n";
			body += "  <input type=\"hidden\" name=\"command\" value=\"\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"key\" value=\"" + key + "\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"timeout\" value=\"0\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"zip\" value=\"1\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"username\" value=\"" + response.credentials.username + "\"/>\r\n";
			body += "  <input type=\"hidden\" name=\"password\" value=\"" + response.credentials.password + "\"/>\r\n";
			body += "  <p><a href=\"#\" onclick=\"document.access.command.value='commit';document.access.submit();return false;\">Commit</a></p>\r\n";
			body += "  <p><a href=\"#\" onclick=\"document.access.command.value='rollback';document.access.submit();return false;\">Rollback</a></p>\r\n";
			body += "  </form>\r\n";
		} else {
			if (home != null)
				body += "  <p><a href=\"" + home + "\">Home</a></p>";
		}
		body += "  </body>\r\n";
		body += "</html>\r\n";
		return body;
	}

	private String showDone() {
		String body = "<html>\r\n";
		body += "  <head>\r\n";
		body += "    <title>SQL Output</title>\r\n";
		if (css != null)
			body += "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + css + "\"/>\r\n";
		body += "  </head>\r\n";
		body += "  <body>\r\n";
		body += "  <p>Done.</p>";
		if (home != null)
			body += "  <p><a href=\"" + home + "\">Home</a></p>";
		body += "  </body>\r\n";
		body += "</html>\r\n";
		return body;
	}

	private String showState(boolean committed, int affected) {
		String body = "<html>\r\n";
		body += "  <head>\r\n";
		body += "    <title>SQL Output</title>\r\n";
		if (css != null)
			body += "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + css + "\"/>\r\n";
		body += "  </head>\r\n";
		body += "  <body>\r\n";
		body += "  <p>" + AFFECTED + ": " + affected + "</p>";
		body += "  <p>" + (committed ? COMMITTED + "." : ROLLEDBACK + ".") + "</p>\r\n";
		if (home != null)
			body += "  <p><a href=\"" + home + "\">Home</a></p>";
		body += "  </body>\r\n";
		body += "</html>\r\n";
		return body;
	}

	public String getCharacterSet() {
		return DEFAULT;
	}

	public String getContentType() {
		return HTML;
	}
}
