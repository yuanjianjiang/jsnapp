package org.jsnap.request;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.Scheme;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.DefaultHttpParams;
import org.apache.http.impl.io.PlainSocketFactory;
import org.apache.http.message.HttpPost;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.util.EntityUtils;
import org.jsnap.db.base.DbParam;
import org.jsnap.exception.JSnapException;
import org.jsnap.exception.UnhandledException;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.exception.comm.MalformedRequestException;
import org.jsnap.exception.security.AccessDeniedException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.SecurityException;
import org.jsnap.http.base.HttpParameters;
import org.jsnap.http.base.HttpReject;
import org.jsnap.http.base.HttpResponse;
import org.jsnap.http.base.HttpServerConnection;
import org.jsnap.http.base.HttpServlet;
import org.jsnap.response.Formatter;
import org.jsnap.response.Response;
import org.jsnap.security.Credentials;
import org.jsnap.util.JUtility;

public class HttpRequest extends DbRequest {
	private static final long serialVersionUID = 2042340568872727567L;

	protected static final String EXECUTE_REQUEST_URI = "/execute";

	private static final String COMMAND = "command";
	private static final String DATABASE = "database";
	private static final String SQL = "sql";
	private static final String FORMATTER = "formatter";
	private static final String TIMEOUT = "timeout";
	private static final String KEEPALIVE = "keepalive";
	private static final String ZIP = "zip";
	private static final String KEY = "key";
	private static final String FROM = "from";
	private static final String TO = "to";
	private static final String MAXROWS = "maxrows";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	private static final String MAXPARAMETERS = "maxparameters";
	private static final String PARAMETER = "parameter";
	private static final String STATE = "state";
	private static final String TYPE = "type";
	private static final String OTHER = "other";
	private static final String VALUE = "value";
	private static final String ISNULL = "isnull";
	private static final String ON = "on";

	private static final int DEFAULT_HTTP_PORT = 80;
	private static final Scheme SCHEME = new Scheme("http", PlainSocketFactory.getSocketFactory(), DEFAULT_HTTP_PORT);
	protected Scheme getScheme() {
		return SCHEME;
	}

	// Used when instance is in client:
	private int port;
	private String host;
	private org.apache.http.HttpResponse response;
	// Used when instance is in server:
	private RequestHandler servlet;

	public HttpRequest() {	// Default constructor required
		super();			// by the Listener class.
	}

	// Clients should use this constructor to create an instance of HttpRequest.
	public HttpRequest(String host, int port) {
		super(System.currentTimeMillis(), null);	// Don't pass socket to lower levels; all socket
		this.host = host;							// operations will be handled at this level.
		this.port = port;
		this.willHandleMyself = true; // All socket operations will be handled at this level.
	}

	// Clients should not use this to create an instance of HttpRequest. That is because
	// this constructor initiates the servlet object, which means that the instance is to
	// be used by the JSnap server to serve the request.
	private HttpRequest(long acceptedOn, Socket s) throws CommunicationException {
		super(acceptedOn, null);		// Don't pass socket to lower levels; all socket
		this.willHandleMyself = true;	// operations will be handled at this level.
		this.servlet = new RequestHandler(HttpServlet.bindSocketForHttp(s));
	}

	protected Request doCreate(long acceptedOn, Socket s) throws CommunicationException, SecurityException, MalformedRequestException {
		HttpRequest req = new HttpRequest(acceptedOn, s);
		req.servlet.service();
		// This call not only reads and unpacks the request but it also processes
		// the received request and sends the response back. Since everything is
		// complete when this call succeeds, the function returns null. Go to the
		// Listener class to see what happens when create returns null.
		return null;
	}

	protected void doReject(Socket s) {
		try {
			HttpReject reject = new HttpReject(HttpServlet.bindSocketForHttp(s));
			reject.service();
		} catch (CommunicationException ignore) {
			ignore.log(); // Hard to handle at this point, just log and ignore.
		}
	}

	protected void processResponse(Response resp) throws JSnapException {
		resp.asBytes(servlet.response.out);
		servlet.response.contentType = resp.getContentType();
		servlet.response.characterSet = resp.getCharacterSet();
		servlet.response.statusCode = HttpStatus.SC_OK; // HTTP.200
	}

	protected void processException(JSnapException ex) {
		ex.log();
		int statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR; // HTTP.500
		if (ex instanceof MalformedRequestException)
			statusCode = HttpStatus.SC_BAD_REQUEST; // HTTP.400
		else if (ex instanceof LoginFailedException || ex instanceof AccessDeniedException)
			statusCode = HttpStatus.SC_UNAUTHORIZED; // HTTP.401
		PrintStream ps = new PrintStream(servlet.response.out);
		ps.println("HTTP " + statusCode + " " + HttpStatus.getStatusText(statusCode));
		ps.println();
		ex.printStackTrace(ps);
		servlet.response.contentType = Formatter.PLAIN;
		servlet.response.characterSet = Formatter.DEFAULT;
		servlet.response.statusCode = statusCode;
	}

	public byte[] receive() throws JSnapException {
		return read();
	}

	protected byte[] read() throws CommunicationException {
		// This function gets called only for the client's reads. Server's
		// reads are performed by the RequestHandler.doServiceImpl() method.
		HttpEntity entity = response.getEntity();
		response = null; // Response has been read/consumed.
		try {
			return EntityUtils.toByteArray(entity);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	protected void write(byte[] packed) throws CommunicationException, SecurityException {
		// This function gets called only for the client's writes. Server's
		// writes are performed by the RequestHandler.doServiceImpl() method.
        HttpHost host = new HttpHost(this.host, this.port, getScheme());
        HttpClientConnection connection = new DefaultHttpClientConnection(host);
		HttpPost post = new HttpPost(EXECUTE_REQUEST_URI);
		post.setEntity(new ByteArrayEntity(packed));
		HttpRequestExecutor executor = new HttpRequestExecutor();
        executor.addInterceptor(new RequestContent());
        executor.addInterceptor(new RequestTargetHost());
		// This is a very reliable timeout mechanism for the _client_. Recall that
		// server's timeout mechanism depends on the JDBC driver. 
		if (timeout != 0) {
			int remaining = new Long((tryUntil + timeout) - System.currentTimeMillis()).intValue();
			if (remaining < 0)
				remaining = 1; // immediately!
			HttpParams params = new DefaultHttpParams();
			params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, remaining);
			executor.setParams(params);
		}
        try {
			response = executor.execute(post, connection);
		} catch (IOException e) {
			throw new CommunicationException(e);
		} catch (HttpException e) {
			throw new CommunicationException(e);
		}
	}

	protected byte[] doPack() {
		try {
			String packed = COMMAND + "=" + URLEncoder.encode(JUtility.valueOf(command, ""), Formatter.DEFAULT) + "&";
			packed += DATABASE + "=" + URLEncoder.encode(JUtility.valueOf(database, ""), Formatter.DEFAULT) + "&";
			packed += SQL + "=" + URLEncoder.encode(JUtility.valueOf(sql, ""), Formatter.DEFAULT) + "&";
			packed += FORMATTER + "=" + URLEncoder.encode(JUtility.valueOf(formatter, ""), Formatter.DEFAULT) + "&";
			packed += TIMEOUT + "=" + Long.toString(timeout) + "&";
			packed += KEEPALIVE + "=" + Long.toString(keepalive) + "&";
			packed += ZIP + "=" + Integer.toString(zip) + "&";
			packed += KEY + "=" + Integer.toString(key) + "&";
			packed += FROM + "=" + Integer.toString(from) + "&";
			packed += TO + "=" + Integer.toString(to) + "&";
			packed += MAXROWS + "=" + Integer.toString(maxrows) + "&";
			String[] credentialsArray = credentials.get();
			packed += USERNAME + "=" + URLEncoder.encode(credentialsArray[0], Formatter.DEFAULT) + "&";
			packed += PASSWORD + "=" + URLEncoder.encode(credentialsArray[1], Formatter.DEFAULT) + "&";
			packed += MAXPARAMETERS + "=" + Integer.toString(parameters.size());
			for (int i = 1; i <= parameters.size(); ++i) {
				DbParam p = parameters.get(i - 1);
				String parameterName = PARAMETER + i;
				packed += "&" + parameterName + STATE + "=" + ON + "&";
				packed += parameterName + TYPE + "=" + Integer.toString(p.type) + "&";
				packed += parameterName + OTHER + "=" + URLEncoder.encode(p.other, Formatter.DEFAULT) + "&";
				packed += parameterName + VALUE + "=" + URLEncoder.encode(p.value, Formatter.DEFAULT) + "&";
				if (p.isNull)
					packed += parameterName + ISNULL + "=" + Boolean.toString(p.isNull);
			}
			return packed.getBytes();
		} catch (UnsupportedEncodingException e) {
			new UnhandledException(e).log(); // Logged but ignored. Highly unlikely.
			return new byte[0];
		}
	}

	protected void doUnpack(byte[] packed, int offset, int length) {
		String s = new String(packed, offset, length);
		HttpParameters parameters = org.jsnap.http.base.HttpRequest.parseInto(s, '=', '&');
		Credentials credentials = org.jsnap.http.base.HttpRequest.extractCredentials(parameters);
		doUnpack(parameters, credentials);
	}

	private void doUnpack(HttpParameters parameters, Credentials credentials) {
		command = JUtility.valueOf(parameters.get(COMMAND), "");
		database = JUtility.valueOf(parameters.get(DATABASE), "");
		sql = JUtility.valueOf(parameters.get(SQL), "");
		formatter = JUtility.valueOf(parameters.get(FORMATTER), "");
		timeout = JUtility.valueOf(parameters.get(TIMEOUT), -1L);
		keepalive = JUtility.valueOf(parameters.get(KEEPALIVE), -1L);
		zip = JUtility.valueOf(parameters.get(ZIP), -1);
		key = JUtility.valueOf(parameters.get(KEY), -1);
		from = JUtility.valueOf(parameters.get(FROM), -1);
		to = JUtility.valueOf(parameters.get(TO), -1);
		maxrows = JUtility.valueOf(parameters.get(MAXROWS), -1);
		this.credentials = credentials;
		int maxParameters = JUtility.valueOf(parameters.get(MAXPARAMETERS), 0);
		for (int i = 1; i <= maxParameters; ++i) {
			String parameterName = PARAMETER + i;
			String parameterState = parameters.get(parameterName + STATE);
			if (parameterState.equals(ON)) {
				int type = JUtility.valueOf(parameters.get(parameterName + TYPE), DbParam.STRING);
				String other = (type == DbParam.VENDOR_SPECIFIC ? parameters.get(parameterName + OTHER) : "");
				String value = parameters.get(parameterName + VALUE);
				boolean isNull = (parameters.get(parameterName + ISNULL) != null);
				this.parameters.add(new DbParam(type, other, value, isNull));
			}
		}
	}

	private void trim() {
		command = command.trim();
		database = database.trim();
		sql = sql.trim();
		formatter = formatter.trim();
	}

	private class RequestHandler extends HttpServlet {
		private HttpResponse response;

		public RequestHandler(HttpServerConnection c) {
			super(c);
		}

		protected void doServiceImpl(org.jsnap.http.base.HttpRequest request, HttpResponse response) {
			this.response = response;
			try {
				if (request.uri.equals(EXECUTE_REQUEST_URI) == false)
					throw new MalformedRequestException("Requests are welcome only at " + EXECUTE_REQUEST_URI);
				doUnpack(request.parameters, request.credentials);
				check();
				trim();
				run();
			} catch (JSnapException e) {
				processException(e);
			} catch (Throwable t) {
				processException(new UnhandledException(t));
			}
			response.zipSize = (zip < 0 ? 0 : zip);
		}
	}
}
