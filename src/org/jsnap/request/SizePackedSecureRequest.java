package org.jsnap.request;

import java.io.IOException;
import java.net.Socket;

import org.jsnap.exception.comm.CommunicationException;

public final class SizePackedSecureRequest extends SizePackedRequest {
	private static final long serialVersionUID = -3285660620751242027L;

	private final boolean trustAll;

	private static final SSLSocketFactory TRUSTING_FACTORY = SSLSocketFactory.getTrusting();
	private static final SSLSocketFactory VALIDATING_FACTORY = SSLSocketFactory.getValidating();

	public SizePackedSecureRequest() {	// Default constructor required
		super();						// by the Listener class.
		this.trustAll = false;
	}

	// Clients should use this constructor to create an instance of SecureSizePackedRequest.
	public SizePackedSecureRequest(String host, int port, boolean trustAll) throws IOException {
		super(host, port);
		this.trustAll = trustAll;
	}

	protected Socket open() throws CommunicationException {
		try {
			return (trustAll ? TRUSTING_FACTORY.createSocket(host, port) : VALIDATING_FACTORY.createSocket(host, port));
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	public boolean secure() {
		return true;
	}
}
