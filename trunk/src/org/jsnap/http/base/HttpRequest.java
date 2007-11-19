package org.jsnap.http.base;

import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;

import org.jsnap.response.Formatter;
import org.jsnap.security.Credentials;

public final class HttpRequest {
	public final String uri;
	public final Credentials credentials;
	public final HttpParameters cookies;
	public final HttpParameters parameters;

	public HttpRequest(String uri, Socket socket, String data, String cookieLine) {
		this.uri = uri;
		cookies = parseInto(cookieLine, '=', ';');
		parameters = parseInto(data, '=', '&');
		credentials = extractCredentials(parameters);
		credentials.setIpAddress(socket);
	}

	public static HttpParameters parseInto(String lineToParse, int equalsChar, int separatorChar) {
		HttpParameters map = new HttpParameters();
		String line = lineToParse;
		int length = line.length();
		while (length > 0) {
			int ix = line.indexOf(separatorChar);
			String pair;
			if (ix < 0) {
				pair = line;
				line = "";
			} else {
				pair = line.substring(0, ix);
				line = line.substring(ix + 1);
			}
			ix = pair.indexOf(equalsChar);
			String key, value;
			try {
				if (ix < 0) {
					key = URLDecoder.decode(pair, Formatter.DEFAULT);
					value = "";
				} else {
					key = URLDecoder.decode(pair.substring(0, ix), Formatter.DEFAULT);
					value = URLDecoder.decode(pair.substring(ix + 1), Formatter.DEFAULT);
				}
				map.put(key.trim(), value.trim());
			} catch (UnsupportedEncodingException ignore) {
			}
			length = line.length();
		}
		return map;
	}

	public static Credentials extractCredentials(HttpParameters parameters) {
		String username = parameters.get(org.jsnap.request.HttpRequest.USERNAME);
		String password = parameters.get(org.jsnap.request.HttpRequest.PASSWORD);
		return new Credentials(username, password);
	}
}
