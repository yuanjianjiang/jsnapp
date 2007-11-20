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
