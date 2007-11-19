package org.jsnap.http.pages;

import org.jsnap.db.base.Dbregistry;

public final class WebAuthenticationStats extends AuthenticationStats {
	public static final WebAuthenticationStats INSTANCE = new WebAuthenticationStats();

	public String logKey() {
		return "auth-web";
	}

	public String key() {
		return "stats-auth-web";
	}

	public String name() {
		return "Authentication Statistics (Web Console)";
	}

	private WebAuthenticationStats() {
		super(Dbregistry.INTERNALDB_NAME);
	}
}
