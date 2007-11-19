package org.jsnap.response;

import java.util.StringTokenizer;

import org.jsnap.db.base.DbHeader;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.exception.comm.MalformedResponseException;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;

public final class CSVFormatter implements Formatter {
	private static final String DICTIONARY = "Dictionary";
	private static final String RESULTSET = "ResultSet";

	private boolean displayHeader = true;
	private String fieldSeparator = ";";
	private String lineSeparator = System.getProperty("line.separator");

	public void setParameter(String name, String value) {
		if (name.equals("comma"))
			fieldSeparator = value;
		else if (name.equals("line"))
			lineSeparator = value;
		else if (name.equals("header"))
			displayHeader = Boolean.valueOf(value);
	}

	public byte[] format(Response response) throws ResultSetException {
		return formatAsObject(response).getBytes();
	}

	public String formatAsObject(Response response) throws ResultSetException {
		String body;
		int key = response.getKey();
		DbResultSet result = response.getResult();
		JPair<Boolean, Boolean> state = response.getOwnerState();
		if (state == null || state.first == true) {
			if (result == null) {
				body = showKey(key);
			} else {
				if (result.hasCursor())
					body = showCursor(result);
				else
					body = showAffected(result, key);
			}
		} else {
			if (result == null)
				body = showDone();
			else
				body = showState(state.second, result.affectedRows());
		}
		return body;
	}

	public int extractKey(byte[] response) throws MalformedResponseException {
		StringTokenizer tokenizer = new StringTokenizer(new String(response), lineSeparator);
		if (tokenizer.countTokens() != 2)
			throw new MalformedResponseException("Token count mismatch");
		String header = tokenizer.nextToken();
		String key = tokenizer.nextToken();
		if (header.equals(KEY) == false)
			throw new MalformedResponseException("First token is not " + KEY);
		int k = JUtility.valueOf(key, -1);
		if (k <= 0)
			throw new MalformedResponseException("Key value is not positive or non-number");
		return k;
	}

	private String showKey(int key) {
		return KEY + lineSeparator + Integer.toString(key) + lineSeparator;
	}

	private String showCursor(DbResultSet result) throws ResultSetException {
		String body;
		if (displayHeader) {
			body = DICTIONARY + lineSeparator;
			int columnCount = result.getColumnCount();
			for (int i = 1; i <= columnCount; ++i) {
				DbHeader definition = result.whatis(i);
				body += definition.name + fieldSeparator +
						definition.type.getSimpleName() + fieldSeparator +
						Integer.toString(definition.precision) + fieldSeparator +
						Integer.toString(definition.scale) + lineSeparator;
			}
		} else {
			body = "";
		}
		String tbody = "";
		int rowCount = 0;
		int columnCount = result.getColumnCount();
		while (result.next()) {
			for (int i = 1; i <= columnCount; ++i) {
				tbody += result.get(i).toString();
				if (i < columnCount)
					tbody += fieldSeparator;
			}
			tbody += lineSeparator;
			++rowCount;
		}
		if (displayHeader)
			body += ROWS + lineSeparator + Integer.toString(rowCount) + lineSeparator + RESULTSET + lineSeparator;
		body += tbody;
		return body;
	}

	public String showAffected(DbResultSet result, int key) {
		String body = (key > 0 ? KEY + lineSeparator + Integer.toString(key) + lineSeparator : "");
		body += AFFECTED + lineSeparator + Integer.toString(result.affectedRows()) + lineSeparator;
		return body;
	}

	private String showDone() {
		return DONE + lineSeparator;
	}

	private String showState(boolean committed, int affected) {
		return STATE + lineSeparator + (committed ? COMMITTED : ROLLEDBACK) + lineSeparator;
	}

	public String getCharacterSet() {
		return DEFAULT;
	}

	public String getContentType() {
		return PLAIN;
	}
}
