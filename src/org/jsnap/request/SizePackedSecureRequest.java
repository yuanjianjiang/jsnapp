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
