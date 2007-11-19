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
