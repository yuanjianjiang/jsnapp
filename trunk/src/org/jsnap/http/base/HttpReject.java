package org.jsnap.http.base;

import java.io.PrintWriter;

import org.apache.http.HttpStatus;
import org.jsnap.response.Formatter;

public class HttpReject extends HttpServlet {
	public HttpReject(HttpServerConnection c) {
		super(c);
	}

	protected void doServiceImpl(HttpRequest request, HttpResponse response) {
		response.statusCode = HttpStatus.SC_SERVICE_UNAVAILABLE;
		response.contentType = Formatter.PLAIN;
		response.characterSet = Formatter.ISO_8859_1;
		PrintWriter writer = new PrintWriter(response.out);
		writer.println("Server is too busy to handle your request. Please retry at a later time.");
	}
}
