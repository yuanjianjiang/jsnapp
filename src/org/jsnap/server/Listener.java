package org.jsnap.server;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.JSnapException;
import org.jsnap.exception.server.ListenerCreateException;
import org.jsnap.exception.server.ListenerIOException;
import org.jsnap.exception.server.ListenerStartException;
import org.jsnap.request.Request;
import org.jsnap.server.Workers.Worker;

public class Listener implements Runnable {
	@SuppressWarnings("unchecked")
	public static Listener create(int port, int backlog, String accepts, Workers workers) throws ListenerCreateException {
		if (port <= 0 || port > 65535)
			throw new ListenerCreateException(port + " is not a valid port value");
		if (backlog <= 0)
			throw new ListenerCreateException(backlog + " is not a valid backlog value");
		try {
			Class rc = Class.forName(accepts);
			if (Request.class.isAssignableFrom(rc) == false)
				throw new ListenerCreateException(accepts + " does not implement the Request interface");
			Constructor ctor = rc.getConstructor(new Class[]{});			// This instance is used only to call
			Request request = (Request)ctor.newInstance(new Object[]{});	// the create() method.
			if (request.secure())
				return new SSLListener(port, backlog, request, workers);
			else
				return new Listener(port, backlog, request, workers);
		} catch (ClassNotFoundException e) {
			throw new ListenerCreateException(e);
		} catch (NoSuchMethodException e) {
			throw new ListenerCreateException(e);
		} catch (java.lang.SecurityException e) {
			throw new ListenerCreateException(e);
		} catch (IllegalArgumentException e) {
			throw new ListenerCreateException(e);
		} catch (InstantiationException e) {
			throw new ListenerCreateException(e);
		} catch (IllegalAccessException e) {
			throw new ListenerCreateException(e);
		} catch (InvocationTargetException e) {
			throw new ListenerCreateException(e);
		}
	}

	// Use Listener.create to instantiate a Listener.
	protected Listener(int port, int backlog, Request request, Workers workers) {
		this.port = port;
		this.backlog = backlog;
		this.request = request;
		this.workers = workers;
		this.socket = null;
		this.pillow = new Object();
	}

	public boolean equals(Object obj) {
		if (this.getClass() == obj.getClass()) {
			Listener op = (Listener)obj;
			return (port == op.port &&
					backlog == op.backlog &&
					request.getClass().equals(op.request.getClass()));
		}
		return false;
	}

	public ListenerInfo info() {
		return new ListenerInfo();
	}

	public boolean secure() {
		return false;
	}

	public void start() throws ListenerStartException {
		synchronized (pillow) {
			try {
				socket = new ServerSocket(port, backlog);
			} catch (IOException e) {
				throw new ListenerStartException(port, e);
			}
			pillow.notify();
		}
	}

	public void stop() {
		Logger.getLogger(this.getClass()).log(Level.DEBUG, "Closing server socket");
		try {
			socket.close();
		} catch (IOException e) {
			Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an I/O exception", e);
		} finally {
			socket = null;
		}
	}

	public void run() {
		Logger.getLogger(this.getClass()).log(Level.INFO, "Up and running");
		Logger.getLogger(this.getClass()).log(Level.DEBUG, "backlog: " + backlog);
		Logger.getLogger(this.getClass()).log(Level.DEBUG, "accepts: " + request.getClass().getSimpleName());
		waitUntilBound();
		while (socket != null && socket.isClosed() == false) {
			try {
				final Socket s = socket.accept();
				Logger.getLogger(this.getClass()).log(Level.DEBUG, "Accepted a new connection from " + s.getInetAddress().getHostAddress());
				// Trying to make sure that the listener goes back to accepting
				// connections as quick as possible: everything is handled
				// by a worker thread once a connection is accepted.
				try {
					workers.execute(
							new Runnable() {
								public void run() {
									try {
										long now = System.currentTimeMillis();
										Request req = request.create(now, s);
										// Some implementations may purposefully return null which
										// must be handled gracefully. Those implementations choose
										// to process the request themselves. (See HttpRequest.)
										if (req != null)
											req.run();
										long passed = System.currentTimeMillis() - now;
										Logger.getLogger(Worker.class).log(Level.DEBUG, "Processing the request took " + passed + " millis in total");
									} catch (JSnapException e) {
										e.log(); // logged and skipped.
									}
								}
							}
						);
				} catch (RejectedExecutionException e) {
					// Assuming a fast implementation that will not keep
					request.reject(s); // the listener busy for too long.
				}
				// Accepted socket is closed in Request context, either in
				// Request.create(), Request.reject() or Request.run().
			} catch (IOException e) {
				new ListenerIOException(port, e).log(); // logged and skipped.
			}
		}
		Logger.getLogger(this.getClass()).log(Level.INFO, "Terminated");
	}

	private void waitUntilBound() {
		Logger.getLogger(this.getClass()).log(Level.DEBUG, "Waiting for server socket to get bound");
		synchronized (pillow) {
			while (socket == null) {
				try {
					pillow.wait();
				} catch (InterruptedException ignore) {
					Logger.getLogger(this.getClass()).log(Level.DEBUG, "Ignored an interrupt");
				}
			}
		}
		Logger.getLogger(this.getClass()).log(Level.DEBUG, "Server socket is bound");
	}

	public class ListenerInfo {
		public final int listeningTo;
		public final String accepts;

		public ListenerInfo() {
			listeningTo = port;
			accepts = request.getClass().getName();
		}
	}

	public final int port;
	protected final int backlog;
	protected ServerSocket socket;
	protected final Object pillow; // used to avoid race conditions between threads upon access to socket.
	private final Workers workers;
	private final Request request;
}
