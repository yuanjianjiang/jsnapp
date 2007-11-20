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

package org.jsnap.http.base;

import java.io.IOException;
import java.net.Socket;

import org.apache.http.impl.DefaultHttpParams;
import org.jsnap.exception.UnhandledException;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.request.Request;

public abstract class HttpServletRunner implements Request {
	protected HttpServlet servlet;

	public Request create(long acceptedOn, Socket s) throws CommunicationException, UnhandledException {
		boolean exception = false;
		try {
			HttpServerConnection connection = HttpServlet.bindSocketForHttp(s);
			Request request = create(connection);
			return request;
		} catch (CommunicationException e) {
			exception = true;
			throw e;
		} catch (Throwable t) {
			exception = true;
			throw new UnhandledException(t);
		} finally {
			if (exception) {
				try {
					s.close();
				} catch (IOException e) {
					// Do not even bother to log.
				}
			}
		}
	}

	protected abstract Request create(HttpServerConnection connection);

	public void reject(Socket s) {
		try {
			HttpServerConnection connection = new HttpServerConnection();
			connection.bind(s, new DefaultHttpParams());
			HttpReject reject = new HttpReject(connection);
			reject.service(); // Calls HttpReject.doServiceImpl at some point.
		} catch (IOException ignore) {
			// Hard to handle at this point, just ignore.
		} finally {
			try {
				s.close();
			} catch (IOException e) {
				// Do not even bother to log.
			}
		}
	}

	public boolean secure() {
		return false;
	}

	public void run() {
		try {
			servlet.service(); // Calls WebInterface.doServiceImpl at some point.
		} catch (Throwable t) {
			servlet.destroy();
			new UnhandledException(t).log();
		}
	}
}
