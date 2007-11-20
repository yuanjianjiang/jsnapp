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

import java.io.PrintWriter;

import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.comm.MalformedRequestException;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.security.PasswordRenewalException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.http.base.HttpResponse;
import org.jsnap.http.base.HttpServerConnection;
import org.jsnap.http.base.HttpServlet;
import org.jsnap.http.base.HttpServletRunner;
import org.jsnap.request.Request;
import org.jsnap.response.Formatter;
import org.jsnap.server.Workers.Worker;
import org.jsnap.util.JUtility;

public class PasswordRenewalRequest extends HttpServletRunner {
	protected Request create(HttpServerConnection connection) {
		PasswordRenewalRequest request = new PasswordRenewalRequest();
		request.servlet = new PasswordRenewalServlet(connection);
		return request;
	}

	public boolean secure() {
		return true;
	}

	private static class PasswordRenewalServlet extends HttpServlet {
		private static final String URI_RENEW = "/renew";

		private Worker executingThread;

		public PasswordRenewalServlet(HttpServerConnection c) {
			super(c);
			executingThread = (Worker)Thread.currentThread();
		}

		protected void doServiceImpl(HttpRequest request, HttpResponse response) {
			Logger.getLogger(PasswordRenewalServlet.class).log(Level.DEBUG, "Request URI is " + request.uri);
			try {
				if (request.uri.equals(URI_RENEW) == false)
					throw new MalformedRequestException("Requests are welcome only at " + URI_RENEW);
				long tryUntil = System.currentTimeMillis() + WEB_DB_TIMEOUT;
				String newPassword = JUtility.valueOf(request.parameters.get("newpassword"), "");
				DbInstance dbi = null;
				try {
					long remaining = tryUntil - System.currentTimeMillis();
					executingThread.changePassword(request.credentials, newPassword, remaining);
				} catch (ConnectException e) {
					throw new PasswordRenewalException(e);
				} finally {
					if (dbi != null)
						dbi.close();
				}
				// Password successfully renewed.
				response.contentType = Formatter.PLAIN;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_OK;
				response.zipSize = 0; // Output will not get gzipped.
			} catch (Throwable t) {
				PrintWriter writer = new PrintWriter(response.out);
				t.printStackTrace(writer);
				writer.flush();
				response.contentType = Formatter.PLAIN;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				response.zipSize = 1; // Output is gzipped.
			}
		}
	}
}
