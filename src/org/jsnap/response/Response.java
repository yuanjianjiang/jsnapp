package org.jsnap.response;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jsnap.db.base.DbResultSet;
import org.jsnap.exception.comm.CommunicationException;
import org.jsnap.exception.comm.FormatterCreateException;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.response.ResponseTracker.StoredResponse;
import org.jsnap.security.Credentials;
import org.jsnap.util.JPair;

public class Response {
	public final Credentials credentials;
	private final Formatter formatter;
	private final DbResultSet result;
	private boolean resultVisible;
	private StoredResponse owner;
	private int key;

	@SuppressWarnings("unchecked")
	public static Formatter getFormatter(String formatterString) throws FormatterCreateException {
		String formatterClass, formatterData;
		int pos = formatterString.indexOf('?');
		if (pos < 0) {
			formatterClass = formatterString;
			formatterData = null;
		} else {
			formatterClass = formatterString.substring(0, pos);
			formatterData = formatterString.substring(pos + 1);
		}
		try {
			Class fc = Class.forName(formatterClass);
			if (Formatter.class.isAssignableFrom(fc) == false)
				throw new FormatterCreateException(formatterClass + " does not implement the Formatter interface");
			Constructor ctor = fc.getConstructor(new Class[]{});
			Formatter formatter = (Formatter)ctor.newInstance(new Object[]{});
			formatterParameters(formatter, formatterData);
			return formatter;
		} catch (ClassNotFoundException e) {
			throw new FormatterCreateException(e);
		} catch (NoSuchMethodException e) {
			throw new FormatterCreateException(e);
		} catch (SecurityException e) {
			throw new FormatterCreateException(e);
		} catch (IllegalArgumentException e) {
			throw new FormatterCreateException(e);
		} catch (InstantiationException e) {
			throw new FormatterCreateException(e);
		} catch (IllegalAccessException e) {
			throw new FormatterCreateException(e);
		} catch (InvocationTargetException e) {
			throw new FormatterCreateException(e);
		}
	}

	public static Formatter getXMLFormatter(String parameterString) {
		Formatter formatter = new XMLFormatter();
		formatterParameters(formatter, parameterString);
		return formatter;
	}

	private static void formatterParameters(Formatter formatter, String parameterString) {
		if (parameterString != null) {
			String[] parameters = parameterString.split("&");
			for (String parameter: parameters) {
				String name, value;
				int pos = parameter.indexOf('=');
				if (pos < 0) {
					name = parameter;
					value = "";
				} else {
					name = parameter.substring(0, pos);
					value = parameter.substring(pos + 1);
				}
				formatter.setParameter(name, value);
			}
		}
	}

	public Response(Credentials credentials, DbResultSet result, Formatter formatter) {
		this.credentials = credentials;
		this.formatter = formatter;
		this.resultVisible = false;
		this.result = result;
		this.owner = null;
		this.key = -1;
	}

	public Response(Credentials credentials, DbResultSet result, String formatterString) throws FormatterCreateException {
		this(credentials, result, getFormatter(formatterString));
	}

	public void setKey(int key) {
		this.key = key;
	}

	public void setResultSetVisibility(boolean on) {
		resultVisible = on;
	}

	public void setMarks(int from, int to) throws ResultSetException {
		if (result != null && result.hasCursor()) {
			if (to == 0)
				result.positionAt(from);
			else
				result.limitBetween(from, to);
		}
	}

	public void setOwner(StoredResponse sr) {
		owner = sr;
	}

	protected int getKey() {
		return key;
	}

	protected DbResultSet getResult() {
		if (result == null) {
			return null;
		} else if (result.hasCursor()) {
			return (resultVisible ? result : null);
		} else {
			return result;
		}
	}

	public JPair<Boolean, Boolean> getOwnerState() {
		if (owner != null) {
			synchronized (owner) {
				return new JPair<Boolean, Boolean>(owner.pending(), owner.committed());
			}
		}
		return null;
	}

	public void asBytes(OutputStream os) throws CommunicationException, ResultSetException {
		try {
			byte[] bytes = formatter.format(this);
			os.write(bytes);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	public byte[] asByteArray() throws ResultSetException {
		return formatter.format(this);
	}

	public Object asObject() throws ResultSetException {
		return formatter.formatAsObject(this);
	}

	public void close() {
		if (result != null)
			result.close();
	}

	public String getCharacterSet() {
		return formatter.getCharacterSet();
	}

	public String getContentType() {
		return formatter.getContentType();
	}
}
