package org.jsnap.security;

import org.jsnap.db.base.DbInstance;
import org.jsnap.http.pages.WebPage;

public final class AllowAll implements AuthenticationPolicy, AccessControlPolicy {
	public void setOwnerName(String dbname) {
	}

	public WebPage[] getPages() {
		return new WebPage[]{};
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname) {
		return new User(credentials);
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname, long timeout) {
		return new User(credentials);
	}

	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql) {
	}

	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql, long timeout) {
	}
}
