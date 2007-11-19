package org.jsnap.http.base;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpStatus;
import org.apache.http.util.DateUtils;
import org.jsnap.response.Formatter;

public class HttpResponse {
	private static final HttpDate dateGenerator = new HttpDate();

	public int zipSize;
	public int statusCode;
	public String redirectTo;
	public String contentType;	// Use constants under Formatter. They're HTTP compatible.
	public String characterSet;	// Use constants under Formatter. They're HTTP compatible.
	public final ArrayList<Cookie> cookies;
	public final OutputStream out;

	public HttpResponse(OutputStream out) {
		this.zipSize = 0; // Do not zip.
		this.statusCode = HttpStatus.SC_NO_CONTENT;
		this.redirectTo = null;
		this.contentType = Formatter.PLAIN;  // default.
		this.characterSet = Formatter.DEFAULT; // default.
		this.cookies = new ArrayList<Cookie>();
		this.out = out;
	}

	private static class HttpDate {
		private final DateFormat dateFormat;

		public HttpDate() {
			dateFormat = new SimpleDateFormat(DateUtils.PATTERN_RFC1123, Locale.US);
			dateFormat.setTimeZone(DateUtils.GMT);
		}

		public String getDate(long millis) {
			return dateFormat.format(new Date(millis));
	    }
	}

	public static class Cookie {
		public final String name;
		public final String value;
		public final String path;
		public final String expires;

		public Cookie(String name, String value, String path, long ttl) {
			this.name = (name == null ? null : name.trim());
			this.value = (value == null ? null : value.trim());
			this.path = (path == null ? null : path.trim());
			long now = System.currentTimeMillis();
			this.expires = dateGenerator.getDate(now + ttl);
		}

		public boolean valid() {
			return (name != null && value != null);
		}

		public String toString() {
			String c = "";
			if (name != null && value != null) {
				c = name + "=" + value;
				if (path != null)
					c += ";" + "path=" + path;
				c += ";" + "expires=" + expires;
			}
			return c;
		}
	}
}
