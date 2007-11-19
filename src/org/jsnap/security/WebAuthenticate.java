package org.jsnap.security;

import org.jsnap.http.pages.AuthenticationStats;
import org.jsnap.http.pages.RenewalStats;
import org.jsnap.http.pages.WebAuthenticationStats;
import org.jsnap.http.pages.WebPage;

public final class WebAuthenticate extends Authenticate {
	protected String authKey() {
		return "auth-web";
	}

	public WebPage[] getPages() {
		return new WebPage[] { WebAuthenticationStats.INSTANCE,
							   RenewalStats.INSTANCE,
							   AuthenticationStats.INSTANCE };
	}
}
