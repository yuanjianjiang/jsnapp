package org.jsnap.http.base;

import java.net.Socket;

import org.apache.http.impl.DefaultHttpServerConnection;

public class HttpServerConnection extends DefaultHttpServerConnection {
	public Socket getSocket() {
		return socket;
	}
}