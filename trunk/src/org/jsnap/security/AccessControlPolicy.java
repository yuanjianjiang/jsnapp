package org.jsnap.security;

import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.security.AccessDeniedException;
import org.jsnap.http.pages.WebPage;

public interface AccessControlPolicy {
	// DEV: Implement access control techniques.

	public void setOwnerName(String dbname);
	public WebPage[] getPages();

	// These two methods must be implemented in a thread-safe fashion.
	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql) throws AccessDeniedException;
	public void allowAccess(DbInstance dbi, Credentials credentials, String dbname, String sql, long timeout) throws AccessDeniedException;
}
