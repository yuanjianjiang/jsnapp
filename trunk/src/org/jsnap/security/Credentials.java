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

package org.jsnap.security;

import java.net.Socket;

import org.jsnap.util.JUtility;

public final class Credentials {
	public final String username, password;
	private String remoteIp;

	public static final String CREDENTIALS_NOT_SET = "Credentials are not set";
	public static final String CREDENTIALS_INCOMPLETE = "Credentials are not complete";

	public Credentials(String[] credentials) {
		username = (credentials != null && credentials.length > 0 ? credentials[0] : null);
		password = (credentials != null && credentials.length > 1 ? credentials[1] : null);
	}

	public Credentials(String username, String password) {
		this.username = JUtility.valueOf(username, "").trim();
		this.password = JUtility.valueOf(password, "").trim();
	}

	public Credentials(Credentials source, String newPassword) {
		this.username = source.username;
		this.password = newPassword;
		this.remoteIp = source.remoteIp;
	}

	public boolean equals(Object obj) {
		if (obj instanceof Credentials) {
			Credentials c = (Credentials)obj;
			return (username.equals(c.username) &&
					password.equals(c.password) &&
					remoteIp != null &&
					c.remoteIp != null &&
					remoteIp.equals(c.remoteIp));
		}
		return false;
	}

	public String[] get() {
		return new String[]{ username, password };
	}

	public String getIpAddress() {
		return remoteIp;
	}

	public void setIpAddress(Socket s) {
		remoteIp = s.getInetAddress().getHostAddress();
	}

	public boolean isComplete(boolean enforceIpCheck) {
		return (username != null && username.length() > 0 && password != null && (enforceIpCheck == false || remoteIp != null));
	}
}
