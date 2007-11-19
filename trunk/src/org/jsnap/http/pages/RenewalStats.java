package org.jsnap.http.pages;

import org.jsnap.db.base.Dbregistry;

public final class RenewalStats extends AuthenticationStats {
	public static final RenewalStats INSTANCE = new RenewalStats();

	public String logKey() {
		return "renew";
	}

	public String key() {
		return "stats-renew";
	}

	public String name() {
		return "Password Renewal Statistics";
	}

	protected RenewalStats() {
		super(Dbregistry.INTERNALDB_NAME);
	}
}
