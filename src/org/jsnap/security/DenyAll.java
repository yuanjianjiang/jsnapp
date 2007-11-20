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
import org.jsnap.exception.security.AccessDeniedException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.PasswordManagementException.Reason;
import org.jsnap.http.pages.WebPage;

public class DenyAll implements AuthenticationPolicy, AccessControlPolicy {
	public void setOwnerName(String dbname) {
	}

	public WebPage[] getPages() {
		return new WebPage[]{};
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname) throws LoginFailedException {
		throw new LoginFailedException(Reason.SUPPLIED, "Authentication request is denied by policy");
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname, long timeout) throws LoginFailedException {
		throw new LoginFailedException(Reason.SUPPLIED, "Authentication request is denied by policy");
	}

	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql) throws AccessDeniedException {
		throw new AccessDeniedException("Access denied by policy");
	}

	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql, long timeout) throws AccessDeniedException {
		throw new AccessDeniedException("Access denied by policy");
	}
}
