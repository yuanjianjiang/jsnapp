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
