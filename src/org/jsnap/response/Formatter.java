package org.jsnap.response;

import org.jsnap.exception.comm.MalformedResponseException;
import org.jsnap.exception.db.ResultSetException;

public interface Formatter {
	// DEV: Implement XLSFormatter.
	// Sets Formatter specific parameters.
	public void setParameter(String name, String value);
	// Formats marked rows of the response's result set.
	public byte[] format(Response response) throws ResultSetException;
	public Object formatAsObject(Response response) throws ResultSetException;
	// Extracts and returns the access key from a response returned by another
	// instance of the same formatter, if any.
	public int extractKey(byte[] response) throws MalformedResponseException;
	// Returns the character set in which the output is formatted.
	// e.g. UTF-8, ISO-8859-1 ...
	public String getCharacterSet();
	// Some character sets:
	public static final String UTF_8 = "UTF-8";
	public static final String ISO_8859_1 = "ISO-8859-1";
	public static final String ISO_8859_9 = "ISO-8859-9";
	public static final String DEFAULT = UTF_8;

	// Returns the content type in which the output is written.
	// e.g. text/plain, text/xml ...
	public String getContentType();
	// Some content types:
	public static final String TEXT = "text";
	public static final String GZIP = "gzip";
	public static final String PLAIN = "text/plain";
	public static final String HTML = "text/html";
	public static final String CSS = "text/css";
	public static final String XSL = "text/xsl";
	public static final String XML = "text/xml";
	public static final String JPEG = "image/jpeg";
	public static final String GIF = "image/gif";
	public static final String PNG = "image/png";
	public static final String BINARY = "application/octet-stream";

	public static final String KEY = "Key";
	public static final String ROWS = "Rows";
	public static final String AFFECTED = "Affected";
	public static final String DONE = "Done";
	public static final String STATE = "State";
	public static final String COMMITTED = "Committed";
	public static final String ROLLEDBACK = "Rolled back";
}
