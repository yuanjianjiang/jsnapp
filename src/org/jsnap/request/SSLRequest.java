package org.jsnap.request;

import org.apache.http.Scheme;

public final class SSLRequest extends HttpRequest {
	private static final long serialVersionUID = 7245926946476886307L;

	public SSLRequest() {	// Default constructor required
		super();			// by the Listener class.
	}

	// Clients should use this constructor to create an instance of SSLRequest.
	public SSLRequest(String host, int port, boolean trustAll) {
		super(host, port);
		if (trustAll)
			trustAllCertificates();
		else
			validateCertificates();
	}

	public boolean secure() {
		return true;
	}

	private static final int DEFAULT_SSL_PORT = 443;
	private static final Scheme TRUSTING_SCHEME = new Scheme("https", SSLSocketFactory.getTrusting(), DEFAULT_SSL_PORT);
	private static final Scheme VALIDATING_SCHEME = new Scheme("https", SSLSocketFactory.getValidating(), DEFAULT_SSL_PORT);
	private Scheme scheme = VALIDATING_SCHEME;

	private void trustAllCertificates() {
		scheme = TRUSTING_SCHEME;
	}

	private void validateCertificates() {
		scheme = VALIDATING_SCHEME;
	}

	protected Scheme getScheme() {
		return scheme;
	}
}
