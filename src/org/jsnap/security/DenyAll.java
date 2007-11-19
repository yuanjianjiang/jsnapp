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
