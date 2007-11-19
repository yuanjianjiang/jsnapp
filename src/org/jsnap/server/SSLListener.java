package org.jsnap.server;

import java.io.IOException;

import javax.net.ssl.SSLServerSocketFactory;

import org.jsnap.exception.server.ListenerStartException;
import org.jsnap.request.Request;

public final class SSLListener extends Listener {
	public SSLListener(int port, int backlog, Request request, Workers workers) {
		super(port, backlog, request, workers);
	}

	public boolean secure() {
		return true;
	}

	public void start() throws ListenerStartException {
		synchronized (pillow) {
			try {
				socket = SSLServerSocketFactory.getDefault().createServerSocket(port, backlog);
			} catch (IOException e) {
				throw new ListenerStartException(port, e);
			}
			pillow.notify();
		}
	}
}
