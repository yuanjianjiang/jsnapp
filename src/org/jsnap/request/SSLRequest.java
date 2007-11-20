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
