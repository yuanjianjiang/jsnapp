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
import org.jsnap.exception.security.PasswordManagementException.Reason;

public class AllowAdministratorsOnly extends Authenticate {
	public User authenticate(DbInstance dbi, Credentials credentials, String dbname) throws LoginFailedException {
		User user = super.authenticate(dbi, credentials, dbname);
		if (user.userType != UserType.ADMINISTRATOR)
			throw new LoginFailedException(Reason.SUPPLIED, "Only administrators are allowed to login");
		return user;
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname, long timeout) throws LoginFailedException {
		User user = super.authenticate(dbi, credentials, dbname, timeout);
		if (user.userType != UserType.ADMINISTRATOR)
			throw new LoginFailedException(Reason.SUPPLIED, "Only administrators are allowed to login");
		return user;
	}
}
