package org.jsnap.request;

import java.net.Socket;

import org.jsnap.exception.JSnapException;

public interface Request extends Runnable {
	// Implementing this interface is sufficient to be accepted and processed by a Listener
	// instance. The implementing class must also provide a default constructor. The Listener
	// instance uses this constructor to create an initial instance of the Request class; it
	// then uses create method of this instance to create other instances of the same class. 
	// 
	// The implementing branch that starts with the DbRequest abstract class is supposed to
	// represent database requests and their BaseRequest.run() method ensures this. Another
	// implementation ConsoleRequest, on the other hand, handles requests for JSnap's web
	// console. The actual purpose of the implementing Request class is determined by the
	// way the create and run methods are coded. reject method determines what the client
	// experiences when the server is too busy to handle the client's request.
	public Request create(long acceptedOn, Socket s) throws JSnapException;
	public void reject(Socket s);
	public boolean secure();
}
