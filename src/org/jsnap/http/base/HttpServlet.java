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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpParams;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HttpGet;
import org.apache.http.message.HttpPost;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.http.base.HttpResponse.Cookie;
import org.jsnap.response.Formatter;

public abstract class HttpServlet extends org.apache.http.protocol.HttpService {
	// Pretends to be a Servlet. :)
	public static final long WEB_DB_TIMEOUT = 20000; // 20 seconds.

	private static final int BUFFER_SIZE = 1024; // 1KB.
	private static HttpParams params = new DefaultHttpParams();
	static {
		params.setIntParameter(HttpConnectionParams.SO_LINGER, 2);
		params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, 10000);
		params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8192);
		params.setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, true);
		params.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true);
		params.setParameter(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
	}

	public static HttpServerConnection bindSocketForHttp(Socket s) throws CommunicationException {
		HttpServerConnection connection = new HttpServerConnection();
		try {
			connection.bind(s, params);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
		return connection;
	}

	private Socket underlying;

	public HttpServlet(HttpServerConnection c) {
		super(c);
		underlying = c.getSocket();
		addInterceptor(new ResponseDate());
		setParams(params);
	}

	public void service() {
		try {
			while (isDestroyed() == false && isActive())
				handleRequest(); // Calls doService() at some point.
		} catch (UnsupportedOperationException ignore) {	// Thrown by SSLSocket.shutdownOutput(),
		} finally {											// catch it so that it does not go unhandled.
			destroy();
		} 
	}

	// HTTP headers that are not defined by Jakarta.
	private static final String LOCATION = "Location";
	private static final String ACCEPT_ENCODING = "Accept-Encoding";
	private static final String CONTENT_ENCODING = "Content-Encoding";
	private static final String COOKIE = "Cookie";
	private static final String SET_COOKIE = "Set-Cookie";

	protected void doService(org.apache.http.HttpRequest request, org.apache.http.HttpResponse response) throws HttpException, IOException {
		// Client might keep the executing thread blocked for very long unless this header is added.
		response.addHeader(new Header(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE));
		// Create a wrapped request object.
		String uri, data;
		String method = request.getRequestLine().getMethod();
		if (method.equals(HttpGet.METHOD_NAME)) {
			BasicHttpRequest get = (BasicHttpRequest)request;
			data = get.getRequestLine().getUri();
			int ix = data.indexOf('?');
			uri = (ix < 0 ? data : data.substring(0, ix));
			data = (ix < 0 ? "" : data.substring(ix + 1));
		} else if (method.equals(HttpPost.METHOD_NAME)) {
			BasicHttpEntityEnclosingRequest post = (BasicHttpEntityEnclosingRequest)request;
			HttpEntity postedEntity = post.getEntity();
			uri = post.getRequestLine().getUri();
			data = EntityUtils.toString(postedEntity);
		} else {
			response.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
			response.setHeader(new Header(HTTP.CONTENT_LEN, "0"));
			return;
		}
		String cookieLine = "";
		if (request.containsHeader(COOKIE)) {
			Header[] cookies = request.getHeaders(COOKIE);
			for (Header cookie: cookies) {
				if (cookieLine.length() > 0)
					cookieLine += "; ";
				cookieLine += cookie.getValue();
			}
		}
		HttpRequest req = new HttpRequest(uri, underlying, data, cookieLine);
		// Create a wrapped response object.
		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
		HttpResponse resp = new HttpResponse(out);
		// Do implementation specific processing.
		doServiceImpl(req, resp);
		out.flush(); // It's good practice to do this.
		// Do the actual writing to the actual response object.
		if (resp.redirectTo != null) {
			// Redirection is requested.
			resp.statusCode = HttpStatus.SC_MOVED_TEMPORARILY;
			response.setStatusCode(resp.statusCode);
			Header redirection = new Header(LOCATION, resp.redirectTo);
			response.setHeader(redirection);
			Logger.getLogger(HttpServlet.class).log(Level.DEBUG, "Status Code: " + Integer.toString(resp.statusCode));
			Logger.getLogger(HttpServlet.class).log(Level.DEBUG, redirection.toString());
		} else {
			// There will be a response entity.
			response.setStatusCode(resp.statusCode);
			HttpEntity entity;
			Header contentTypeHeader;
			boolean text = resp.contentType.startsWith(Formatter.TEXT);
			if (text) { // text/* ...
				entity = new StringEntity(out.toString(resp.characterSet), resp.characterSet);
				contentTypeHeader = new Header(HTTP.CONTENT_TYPE, resp.contentType + HTTP.CHARSET_PARAM + resp.characterSet);
			} else { // application/octet-stream, image/* ...
				entity = new ByteArrayEntity(out.toByteArray());
				contentTypeHeader = new Header(HTTP.CONTENT_TYPE, resp.contentType);
			}
			boolean acceptsGzip = clientAcceptsGzip(request);
			long contentLength = entity.getContentLength();
			// If client accepts gzipped content, the implementing object requested that response
			// gets gzipped and size of the response exceeds implementing object's size threshold
			// response entity will be gzipped.
			boolean gzipped = false;
			if (acceptsGzip && resp.zipSize > 0 && contentLength >= resp.zipSize) {
				ByteArrayOutputStream zipped = new ByteArrayOutputStream(BUFFER_SIZE);
				GZIPOutputStream gzos = new GZIPOutputStream(zipped);
				entity.writeTo(gzos);
				gzos.close();
				entity = new ByteArrayEntity(zipped.toByteArray());
				contentLength = zipped.size();
				gzipped = true;
			}
			// This is where true writes are made.
			Header contentLengthHeader = new Header(HTTP.CONTENT_LEN, Long.toString(contentLength));
			Header contentEncodingHeader = null;
			response.setHeader(contentTypeHeader);
			response.setHeader(contentLengthHeader);
			if (gzipped) {
				contentEncodingHeader = new Header(CONTENT_ENCODING, Formatter.GZIP);
				response.setHeader(contentEncodingHeader);
			}
			response.setEntity(entity);
			// Log critical headers.
			Logger.getLogger(HttpServlet.class).log(Level.DEBUG, "Status Code: " + Integer.toString(resp.statusCode));
			Logger.getLogger(HttpServlet.class).log(Level.DEBUG, contentTypeHeader.toString());
			if (gzipped)
				Logger.getLogger(HttpServlet.class).log(Level.DEBUG, contentEncodingHeader.toString());
			Logger.getLogger(HttpServlet.class).log(Level.DEBUG, contentLengthHeader.toString());
		}
		// Log cookies.
		for (Cookie cookie: resp.cookies) {
			if (cookie.valid()) {
				Header h = new Header(SET_COOKIE, cookie.toString());
				response.addHeader(h);
				Logger.getLogger(HttpServlet.class).log(Level.DEBUG, h.toString());
			}
		}
	}

	private boolean clientAcceptsGzip(org.apache.http.HttpRequest request) {
		boolean accepts = false;
		for (Header header: request.getHeaders(ACCEPT_ENCODING))
			accepts |= (header.getValue().indexOf(Formatter.GZIP) >= 0);
		return accepts;
	}

	protected abstract void doServiceImpl(HttpRequest request, HttpResponse response);
}
