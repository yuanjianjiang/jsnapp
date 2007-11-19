package org.jsnap.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.xerces.util.XML11Char;
import org.jsnap.db.base.DbHeader;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.exception.comm.MalformedResponseException;
import org.jsnap.exception.db.ResultSetException;
import org.jsnap.util.JDocument;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;
import org.xml.sax.SAXException;

public final class XMLFormatter implements Formatter {
	private static final int BUFFER_SIZE = 1024; // 1KB.
	private static final String FIELDS = "fields";
	private static final String FIELD = "field";
	private static final String NAME = "name";
	private static final String PRECISION = "precision";
	private static final String SCALE = "scale";
	private static final String TYPE = "type";
	private static final String AUTO_IDENTIFIER = "jsnap-auto-";
	private static final String XKEY = KEY.toLowerCase();
	private static final String XROWS = ROWS.toLowerCase();
	private static final String XAFFECTED = AFFECTED.toLowerCase();
	private static final String XDONE = DONE.toLowerCase();
	private static final String XCOMMITTED = "committed";
	private static final String XROLLEDBACK = "rolledback";

	private boolean noCaps = false;
	private boolean displayMetadata = true;
	private String rootNodeName = "resultset";
	private String recordNodeName = "record";
	private String xsl = null;

	public void setParameter(String name, String value) {
		if (name.equals("metadata"))
			displayMetadata = Boolean.valueOf(value);
		else if (name.equals("nocaps"))
			noCaps = Boolean.valueOf(value);
		else if (name.equals("resultset"))
			rootNodeName = value;
		else if (name.equals("record"))
			recordNodeName = value;
		else if (name.equals("xsl"))
			xsl = value;
	}

	public byte[] format(Response response) throws ResultSetException {
		JDocument doc = formatAsObject(response);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
		try {
			doc.writeToStream(xsl, baos); // Call permits null as stylesheet.
		} catch (IOException e) {
		} catch (SAXException e) {
		}
		return baos.toByteArray();
	}

	public JDocument formatAsObject(Response response) throws ResultSetException {
		JDocument doc = new JDocument(rootNodeName);
		int key = response.getKey();
		DbResultSet result = response.getResult();
		JPair<Boolean, Boolean> state = response.getOwnerState();
		if (state == null || state.first == true) {
			if (result == null) {
				showKey(doc, key);
			} else {
				if (result.hasCursor())
					showCursor(doc, result);
				else
					showAffected(doc, result, key);
			}
		} else {
			if (result == null)
				showDone(doc);
			else
				showState(doc, state.second, result.affectedRows());
		}
		return doc;
	}

	public int extractKey(byte[] response) throws MalformedResponseException {
		try {
			JDocument doc = JDocument.readFrom(new ByteArrayInputStream(response));
			String key = doc.getFirstTextContent(XKEY);
			if (key == null)
				throw new MalformedResponseException("No key is specified in this response");
			int k = JUtility.valueOf(key, -1);
			if (k <= 0)
				throw new MalformedResponseException("Key value is not positive or non-number");
			return k;
		} catch (SAXException e) {
			throw new MalformedResponseException("XML parser failed: " + e.getMessage());
		} catch (IOException e) {
			throw new MalformedResponseException("I/O exception while parsing: " + e.getMessage());
		}
	}

	private void showKey(JDocument doc, int key) {
		doc.appendTextNode(XKEY, Integer.toString(key));
	}

	private void showCursor(JDocument doc, DbResultSet result)
			throws ResultSetException {
		if (displayMetadata) {
			int columnCount = result.getColumnCount();
			doc.appendTextNode(FIELDS, Integer.toString(columnCount));
			String[] names = new String[] { NAME, PRECISION, SCALE, TYPE };
			String[] data = new String[4];
			for (int i = 1; i <= columnCount; ++i) {
				DbHeader definition = result.whatis(i);
				if (XML11Char.isXML11ValidName(definition.name))
					data[0] = (noCaps ? definition.name.toLowerCase() : definition.name);
				else
					data[0] = AUTO_IDENTIFIER + i;
				data[1] = Integer.toString(definition.precision);
				data[2] = Integer.toString(definition.scale);
				data[3] = definition.type.getCanonicalName();
				doc.appendNodeHierarchyNoCheck(FIELD, names, data);
			}
		}
		int rowCount;
		int columnCount = result.getColumnCount();
		JDocument rows = new JDocument(rootNodeName);
		String[] names = new String[columnCount];
		for (int i = 1; i <= columnCount; ++i) {
			DbHeader definition = result.whatis(i);
			if (XML11Char.isXML11ValidName(definition.name))
				names[i - 1] = (noCaps ? definition.name.toLowerCase() : definition.name);
			else
				names[i - 1] = AUTO_IDENTIFIER + i;
		}
		rowCount = 0;
		String[] data = new String[columnCount];
		while (result.next()) {
			for (int i = 1; i <= columnCount; ++i) {
				Object d = result.get(i);
				data[i - 1] = (d == null ? "" : d.toString());
			}
			rows.appendNodeHierarchyNoCheck(recordNodeName, names, data);
			++rowCount;
		}
		if (displayMetadata)
			doc.appendTextNode(XROWS.toLowerCase(), Integer.toString(rowCount));
		if (rows != null)
			doc.copyFrom(rows);
	}

	public void showAffected(JDocument doc, DbResultSet result, int key) {
		if (key > 0)
			doc.appendTextNode(XKEY.toLowerCase(), Integer.toString(key));
		doc.appendTextNode(XAFFECTED.toLowerCase(), Integer.toString(result.affectedRows()));
	}

	private void showDone(JDocument doc) {
		doc.appendEmptyNode(XDONE.toLowerCase());
	}

	private void showState(JDocument doc, boolean committed, int affected) {
		doc.appendEmptyNode((committed ? XCOMMITTED.toLowerCase() : XROLLEDBACK.toLowerCase()));
	}

	public String getCharacterSet() {
		return DEFAULT;
	}

	public String getContentType() {
		return XML;
	}
}
