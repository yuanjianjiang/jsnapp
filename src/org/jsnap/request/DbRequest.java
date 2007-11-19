package org.jsnap.request;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.exception.JSnapException;
import org.jsnap.exception.UnhandledException;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.exception.comm.MalformedRequestException;
import org.jsnap.exception.db.AccessKeyException;
import org.jsnap.exception.security.SecurityException;
import org.jsnap.response.Formatter;
import org.jsnap.response.Response;
import org.jsnap.response.ResponseTracker;
import org.jsnap.response.ResponseTracker.StoredResponse;
import org.jsnap.security.Credentials;
import org.jsnap.server.Workers.Worker;

public abstract class DbRequest implements Serializable, Request {
	public DbRequest() {
		this(-1, null);
	}

	public DbRequest(long acceptedOn, Socket s) {
		clear();
		this.s = s;
		this.willHandleMyself = false;
		this.tryUntil = acceptedOn; // Will be incremented by timeout value later on.
	}

	public void clear() {
		this.command = null;
		this.database = null;
		this.sql = null;
		this.formatter = null;
		this.parameters = new ArrayList<DbParam>();
		this.credentials = null;
		this.timeout = 0L;
		this.keepalive = 0L;
		this.zip = 0;
		this.key = 0;
		this.from = 0;
		this.to = 0;
		this.maxrows = 0;
	}

	// Implementations that will not handle socket operations, that is
	// willHandleMyself == false, should override this method to
	// provide a way to open _client_ sockets.
	protected Socket open() throws CommunicationException {
		return null;
	}

	// This method returns a Request instance after reading it from the passed socket. Method
	// should be coded as if it was a static method; do not rely on local variables. It is
	// possible for this method to return null, but then the processing of the received
	// request must also take place inside the create call.
	// Method is protected so that callers call the create wrapper instead.
	protected abstract Request doCreate(long acceptedOn, Socket s) throws CommunicationException, SecurityException, MalformedRequestException;

	public Request create(long acceptedOn, Socket s) throws CommunicationException, SecurityException, MalformedRequestException, UnhandledException {
		boolean exception = false;
		try {
			return doCreate(acceptedOn, s);
		} catch (CommunicationException e) {
			exception = true;
			throw e;
		} catch (SecurityException e) {
			exception = true;
			throw e;
		} catch (MalformedRequestException e) {
			exception = true;
			throw e;
		} catch (Throwable t) {
			exception = true;
			throw new UnhandledException(t);
		} finally {
			if (exception) {
				try {
					s.close();
				} catch (IOException ignore) {
					// Do not even bother to log.
				}
			}
		}
	}

	// This method must be implemented to run as quick as possible. Method should be coded as
	// if it was a static method; do not rely on local variables.
	// Method is protected so that callers call the reject wrapper instead.
	protected abstract void doReject(Socket s);

	public void reject(Socket s) {
		try {
			doReject(s);
		} catch (Throwable t) {
			new UnhandledException(t).log(); // This might happen but it is only logged.
		}
		// Make sure that the socket gets closed even if doReject fails.
		try {
			s.close();
		} catch (IOException ignore) {
			// Do not even bother to log.
		}
	}

	public boolean secure() {
		return false;
	}

	public void run() {
		// tryUntil contains the time on which the request is accepted.
		long passed = System.currentTimeMillis() - tryUntil;
		Logger.getLogger(this.getClass()).log(Level.DEBUG, passed + " millis passed during preprocessing");
		try {
			tryUntil += timeout;
			executingThread = (Worker)Thread.currentThread();
			if (accessControl()) {
				if (timeout == 0) {
					executingThread.authenticate(database, credentials);
					executingThread.allowAccess(database, credentials, sql);
				} else {
					long remaining = tryUntil - System.currentTimeMillis();
					executingThread.authenticate(database, credentials, remaining);
					remaining = tryUntil - System.currentTimeMillis();
					executingThread.allowAccess(database, credentials, sql, remaining);
				}
			}
			if (command.equals(EXECUTE))
				execute();
			else if (command.equals(RESERVE))
				reserve();
			else if (command.equals(ACCESS))
				access();
			else if (command.equals(COMMIT))
				commit();
			else if (command.equals(ROLLBACK))
				rollback();
		// Exceptions are logged on the server, sent to the client.
		} catch (JSnapException e) {
			if (willHandleMyself || (s != null && s.isClosed() == false)) // Handle unless socket is closed.
				processException(e);
		} catch (Throwable t) {
			if (willHandleMyself || (s != null && s.isClosed() == false)) // Handle unless socket is closed.
				processException(new UnhandledException(t));
		}
		try {
			if (willHandleMyself == false && s != null)
				s.close(); // A listener thread had accepted this socket, it is closed here.
		} catch (IOException ignore) {
			// Do not even bother to log.
		}
	}

	private boolean accessControl() {
		return (command.equals(EXECUTE) || command.equals(RESERVE));
	}

	private void execute() throws JSnapException {
		DbInstance dbi = null;
		Response response = null;
		try {
			// First create a formatter instance; if this fails,
			// it is not necessary to do any database operation.
			Formatter f = Response.getFormatter(formatter);
			// Obtain a database instance.
			if (timeout == 0) {
				dbi = executingThread.getDbInstance(database);
			} else {
				long remaining = tryUntil - System.currentTimeMillis();
				dbi = executingThread.getDbInstance(database, remaining);
			}
			// Execute the SQL statement and produce the result set.
			DbStatement statement = dbi.createStatement(sql, parameters, false);
			statement.setMaxRows(maxrows);
			if (timeout != 0) {
				long remaining = tryUntil - System.currentTimeMillis();
				statement.setTimeout(remaining);
			}
			DbResultSet result = null;
			boolean successful = false;
			try {
				result = statement.execute();
				dbi.commit();
				// If commit fails, changes are rolled back by the
				// dbi.close() in the outermost finally block.
				successful = true;
			} finally {
				if (successful == false) {
					if (result != null)
						result.close();
				}
			}
			// Create a response instance and process it, i.e. send output to the user.
			response = new Response(credentials, result, f);
			response.setResultSetVisibility(true);
			response.setMarks(from, to);
			processResponse(response);
		} finally {
			if (response != null)
				response.close(); // Closes the underlying DbResultSet.
			if (dbi != null)
				dbi.close(); // Returns instance to the pool of database instances.
		}
	}

	private void reserve() throws JSnapException {
		ResponseTracker tracker = executingThread.getResponseTracker();
		DbInstance dbi = null;
		Response response = null;
		StoredResponse sr = null;
		boolean problem = true;
		try {
			// First create a formatter instance; if this fails,
			// it is not necessary to do any database operation.
			Formatter f = Response.getFormatter(formatter);
			// Obtain a database instance.
			if (timeout == 0) {
				dbi = executingThread.getDbInstance(database);
			} else {
				long remaining = tryUntil - System.currentTimeMillis();
				dbi = executingThread.getDbInstance(database, remaining);
			}
			// Execute the SQL statement and produce the result set.
			DbStatement statement = dbi.createStatement(sql, parameters, true);
			statement.setMaxRows(maxrows);
			if (timeout != 0) {
				long remaining = tryUntil - System.currentTimeMillis();
				statement.setTimeout(remaining);
			}
			DbResultSet result = null;
			boolean successful = false;
			try {
				result = statement.execute();
				successful = true;
			} finally {
				if (successful == false && result != null)
					result.close(); // Happens only if an exception is thrown during execute().
			}
			// Create a response instance.
			// Initially, result set is invisible; client will access the result set
			// with the provided key and the result set will be visible only then.
			response = new Response(credentials, result, f);
			response.setResultSetVisibility(false);
			// Generate a StoredResponse instance. This call also sets response's access
			// key that will be sent to the user.
			// After this call the database instance that owns the result set gets reserved
			// as well, i.e. nobody will be able use this instance until the stored response
			// gets closed. (Keep in mind the fact that the executed SQL statement could be an
			// insert/update/delete waiting for a commit or a rollback.)
			sr = tracker.create(keepalive, response, dbi);
			processResponse(response);
			problem = false;
		} finally {
			if (problem) { // Everything gets closed in the case of an error.
				if (sr != null) {
					sr.close(); // This call does everything necessary, i.e. *.close().
				} else {
					if (response != null)
						response.close(); // Closes the underlying DbResultSet.
					if (dbi != null)
						dbi.close(); // Returns instance to the pool of database instances.
				}
			}
		}
	}

	private void access() throws JSnapException {
		ResponseTracker tracker = executingThread.getResponseTracker();
		StoredResponse sr = tracker.get(key);
		if (sr == null)
			throw new AccessKeyException();
		synchronized (sr) {
			sr.check(credentials);
			sr.response.setMarks(from, to);
			sr.response.setResultSetVisibility(true);
			processResponse(sr.response);
		}
	}

	private void commit() throws JSnapException {
		ResponseTracker tracker = executingThread.getResponseTracker();
		StoredResponse sr = tracker.get(key);
		if (sr == null)
			throw new AccessKeyException();
		synchronized (sr) {
			sr.check(credentials);
			try {
				sr.commit();
				// If commit fails, changes are rolled back by the dbi.close()
				// which takes place in sr.remove() in the finally block.
				sr.response.setResultSetVisibility(false);
				processResponse(sr.response);
			} finally {
				sr.close(); // This call does everything necessary, i.e. *.close().
			}
		}
	}

	private void rollback() throws JSnapException {
		ResponseTracker tracker = executingThread.getResponseTracker();
		StoredResponse sr = tracker.get(key);
		if (sr == null)
			throw new AccessKeyException();
		synchronized (sr) {
			sr.check(credentials);
			try {
				sr.rollback();
				sr.response.setResultSetVisibility(false);
				processResponse(sr.response);
			} finally {
				sr.close(); // This call does everything necessary, i.e. *.close().
			}
		}
	}

	protected abstract void processResponse(Response resp) throws JSnapException;

	protected abstract void processException(JSnapException ex);

	public void setCommand(String command) {
		this.command = command;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}

	public void addParameter(DbParam parameter) {
		this.parameters.add(parameter);
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
		if (willHandleMyself == false && s != null)
			this.credentials.setIpAddress(s); // Takes place in client context.
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public void setKeepalive(long keepalive) {
		this.keepalive = keepalive;
	}

	public void setZip(int zip) {
		this.zip = zip;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public void setFrom(int from) {
		this.from = from;
	}

	public void setTo(int to) {
		this.to = to;
	}

	public void setMaxRows(int maxrows) {
		this.maxrows = maxrows;
	}

	// Intended for clients' use: client creates a Request instance, uses the setter methods to fill
	// in the fields and calls send to communicate with the server. Server's response will be read
	// by the receive method.
	public void send() throws CommunicationException, SecurityException, MalformedRequestException {
		// This is a very reliable timeout mechanism for the _client_. Recall that
		// server's timeout mechanism depends on the JDBC driver.
		if (willHandleMyself == false) {
			s = open();
			if (timeout > 0) {
				int t = new Long(timeout).intValue();
				try {
					s.setSoTimeout(t);
				} catch (SocketException e) {
					throw new CommunicationException(e);
				}
			}
		}
		byte[] packed = pack();
		write(packed);
	}

	// Intended for clients' use: client will use this method to read server's response to the
	// request. Request must be sent first in order for receive to succeed.
	public abstract byte[] receive() throws JSnapException;

	// This function reads request's data from the wire. It determines how data is transferred over the network.
	protected abstract byte[] read() throws CommunicationException, SecurityException;

	// This function puts request's data on the wire. It determines how data is transferred over the network.
	protected abstract void write(byte[] packed) throws CommunicationException, SecurityException;

	// This function packs request's data prior to putting it on the wire.
	public byte[] pack() throws MalformedRequestException {
		check(false);
		return doPack();
	}

	protected abstract byte[] doPack();
	
	// These functions unpack and fill in request's data after reading it from the wire.
	public void unpack(byte[] packed, int offset, int length) throws MalformedRequestException {
		doUnpack(packed, offset, length);
		if (willHandleMyself == false)	 // Server fills in the remote IP address from the socket,
			credentials.setIpAddress(s); // rather than reading it from the request data.
		check(true);
	}

	public void unpack(byte[] packed) throws MalformedRequestException {
		unpack(packed, 0, packed.length);
	}

	public void unpack(byte[] packed, int offset) throws MalformedRequestException {
		unpack(packed, offset, packed.length - offset);
	}

	protected abstract void doUnpack(byte[] packed, int offset, int length);

	// This method checks if the necessary fields are set or not, i.e. if this is a valid request.
	// Checking whether an IP address is set or not is optional according to the parameter.
	private void check(boolean enforceIpCheck) throws MalformedRequestException {
		if (command == null || command.length() == 0)
			throw new MalformedRequestException("Command is not set");
		else if (timeout < 0)
			throw new MalformedRequestException("Timeout duration cannot be negative");
		else if (zip < 0)
			throw new MalformedRequestException("Zip threshold cannot be negative");
		else if (credentials == null)
			throw new MalformedRequestException(Credentials.CREDENTIALS_NOT_SET);
		else if (credentials.isComplete(enforceIpCheck) == false)
			throw new MalformedRequestException(Credentials.CREDENTIALS_INCOMPLETE);
		if (command.equals(EXECUTE)) {
			if (database == null || database.length() == 0)
				throw new MalformedRequestException("Database name is not set");
			else if (sql == null || sql.length() == 0)
				throw new MalformedRequestException("SQL text is not set");
			else if (formatter == null || formatter.length() == 0)
				throw new MalformedRequestException("Output formatter is not set");
			else if (from < 0)
				throw new MalformedRequestException("Initial access point cannot be negative");
			else if (to < 0)
				throw new MalformedRequestException("Final access point cannot be negative");
			else if (to != 0 && from > to)
				throw new MalformedRequestException("Initial access point cannot be greater than final");
			else if (maxrows < 0)
				throw new MalformedRequestException("Maximum number of rows cannot be negative");
		} else if (command.equals(RESERVE)) {
			if (database == null || database.length() == 0)
				throw new MalformedRequestException("Database name is not set");
			else if (sql == null || sql.length() == 0)
				throw new MalformedRequestException("SQL text is not set");
			else if (formatter == null || formatter.length() == 0)
				throw new MalformedRequestException("Output formatter is not set");
			else if (keepalive <= 0)
				throw new MalformedRequestException("Keepalive duration must be positive");
			else if (maxrows < 0)
				throw new MalformedRequestException("Maximum number of rows cannot be negative");
		} else if (command.equals(ACCESS)) {
			if (key <= 0)
				throw new MalformedRequestException("Access key must be positive");
			else if (from < 0)
				throw new MalformedRequestException("Initial access point cannot be negative");
			else if (to < 0)
				throw new MalformedRequestException("Final access point cannot be negative");
			else if (to != 0 && from > to)
				throw new MalformedRequestException("Initial access point cannot be greater than final");
		} else if (command.equals(COMMIT)) {
			if (key <= 0)
				throw new MalformedRequestException("Access key must be positive");
		} else if (command.equals(ROLLBACK)) {
			if (key <= 0)
				throw new MalformedRequestException("Access key must be positive");
		} else {
			throw new MalformedRequestException("Request contains an unsupported command");
		}
	}

	// This protected version makes sure an IP address is set within the request.
	protected void check() throws MalformedRequestException {
		check(true);
	}

	private static final String EXECUTE = "execute";
	private static final String RESERVE = "reserve";
	private static final String ACCESS = "access";
	private static final String COMMIT = "commit";
	private static final String ROLLBACK = "rollback";

	protected String command, database, sql, formatter;
	protected ArrayList<DbParam> parameters;
	protected Credentials credentials;
	protected long timeout, keepalive;
	protected int zip, key, from, to, maxrows;
										// Transient since it is not a serializable object.
	protected transient Socket s;		// readObject and writeObject ignore this variable.
												  // Implementations that will handle socket
	protected transient boolean willHandleMyself; // operations itself should set this flag.
	private transient Worker executingThread;	  // These two are internal variables, there
	protected transient long tryUntil;			  // is no need for them to get serialized.
}
