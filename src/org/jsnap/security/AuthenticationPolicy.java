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
