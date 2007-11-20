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

import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.http.pages.WebPage;

public interface AuthenticationPolicy {
	public void setOwnerName(String dbname);
	public WebPage[] getPages();

	// These methods must be implemented in a thread-safe fashion.
	public User authenticate(DbInstance dbi, Credentials credentials, String dbname) throws LoginFailedException;
	public User authenticate(DbInstance dbi, Credentials credentials, String dbname, long timeout) throws LoginFailedException;

	public static final String NO_DIGEST = "PLAIN";

	public enum UserType { NOT_LOOKED_UP, ADMINISTRATOR, USER }; // Add more if necessary.

	public static class User {
		public final Credentials credentials;
		public final UserType userType;

		public User(Credentials credentials) {
			this.credentials = credentials;
			this.userType = UserType.NOT_LOOKED_UP;
		}

		public User(Credentials credentials, UserType userType) {
			this.credentials = credentials;
			this.userType = userType;
		}
	}
}
