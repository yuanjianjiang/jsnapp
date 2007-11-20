/************************************************************************
 * This file is part of jsnap.                                          *
 *                                                                      *
 * jsnap is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation, either version 3 of the License, or    *
 * (at your option) any later version.                                  *
 *                                                                      *
 * jsnap is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 * GNU General Public License for more details.                         *
 *                                                                      *
 * You should have received a copy of the GNU General Public License    *
 * along with jsnap.  If not, see <http://www.gnu.org/licenses/>.       *
 ************************************************************************/

package org.jsnap.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;

import org.apache.xerces.dom.DocumentImpl;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.jsnap.response.Formatter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class JDocument {
	private Document doc;
	private Element root;

	private static final String SELECTED = "selected";
	private static final String ROOT = "document";
	private static final String FIRST = "first";
	private static final String SECOND = "second";
	private static final String KEY = "key";
	private static final String VALUE = "value";

	public static JDocument readFrom(Reader rd) throws SAXException, IOException {
		InputSource source = new InputSource(rd);
		DOMParser parser = new DOMParser();
		parser.parse(source);
		Document doc = parser.getDocument();
		return new JDocument(doc);
	}

	public static JDocument readFrom(InputStream is) throws SAXException, IOException {
		InputSource source = new InputSource(is);
		DOMParser parser = new DOMParser();
		parser.parse(source);
		Document doc = parser.getDocument();
		return new JDocument(doc);
	}

	private JDocument(Document doc) {
		this.doc = doc;
		root = doc.getDocumentElement();
	}

	public JDocument(String rootName) {
		doc = new DocumentImpl();
		root = doc.createElement(JUtility.valueOf(rootName, ROOT).trim());
		doc.appendChild(root);
	}

	public String getFirstTextContent(String tagname) {
		String value = null;
		NodeList nodes = doc.getElementsByTagName(tagname);
		if (nodes.getLength() > 0)
			value = nodes.item(0).getTextContent();
		return value;
	}

	public String[] getAllTextContent(String tagname) {
		NodeList nodes = doc.getElementsByTagName(tagname);
		String[] values = new String[nodes.getLength()];
		for (int i = 0; i < values.length; ++i)
			values[i] = nodes.item(i).getTextContent();
		return values;
	}

	public void appendEmptyNode(String name) {
		String n = JUtility.valueOf(name, FIRST).trim();
		root.appendChild(doc.createElement(n));
	}

	public void appendTextNode(String name, String data) {
		appendTextNode(name, data, true, null);
	}

	public void appendTextNodeNoCheck(String name, String data) {
		appendTextNode(name, data, false, null);
	}

	public void appendTextNodeWithAttr(String name, String data, JPair<String, String> attribute) {
		appendTextNode(name, data, true, attribute);
	}

	public void appendTextNodeWithAttrNoCheck(String name, String data, JPair<String, String> attribute) {
		appendTextNode(name, data, false, attribute);
	}

	private void appendTextNode(String name, String data, boolean emptyCheck, JPair<String, String> attribute) {
		String n = JUtility.valueOf(name, FIRST).trim();
		String d = JUtility.valueOf(data, "").trim();
		if ((n != null && n.length() > 0) && (emptyCheck == false || (d != null && d.length() > 0))) {
			Element elem = doc.createElement(n);
			if (attribute != null) {
				String k = JUtility.valueOf(attribute.first, KEY).trim();
				String v = JUtility.valueOf(attribute.second, VALUE).trim();
				if (k.length() > 0 && (emptyCheck == false || v.length() > 0))
					elem.setAttribute(k, v);
			}
			elem.appendChild(doc.createTextNode(JUtility.valueOf(d, "")));
			root.appendChild(elem);
		}
	}

	public void appendNodeHierarchy(String rootName, String[] names, String[] data) {
		appendNodeHierarchy(rootName, names, null, data, true);
	}

	public void appendNodeHierarchy(String rootName, String[] names, boolean[] selected, String[] data) {
		appendNodeHierarchy(rootName, names, selected, data, true);
	}

	public void appendNodeHierarchyNoCheck(String rootName, String[] names, String[] data) {
		appendNodeHierarchy(rootName, names, null, data, false);
	}

	public void appendNodeHierarchyNoCheck(String rootName, String[] names, boolean[] selected, String[] data) {
		appendNodeHierarchy(rootName, names, selected, data, false);
	}

	private void appendNodeHierarchy(String rootName, String[] names, boolean[] selected, String[] data, boolean emptyCheck) {
		int count = Math.min(names.length, data.length);
		if (count > 0) {
			Element r = doc.createElement(JUtility.valueOf(rootName, FIRST).trim());
			for (int i = 0; i < count; ++i) {
				String n = JUtility.valueOf(names[i], SECOND).trim();
				String d = JUtility.valueOf(data[i], "").trim();
				String s = (selected != null && i < selected.length ? Boolean.toString(selected[i]) : "");
				if ((n != null && n.length() > 0) && (emptyCheck == false || (d != null && d.length() > 0))) {
					Element elem = doc.createElement(n);
					if (s.length() > 0)
						elem.setAttribute(SELECTED, s);
					elem.appendChild(doc.createTextNode(JUtility.valueOf(d, "")));
					r.appendChild(elem);
				}
			}
			root.appendChild(r);
		}
	}

	public void appendNodeHierarchy(String rootName, String groupName, String first, String second, ArrayList<JPair<Integer, String>> list) {
		appendNodeHierarchy(rootName, groupName, first, second, list, null, true);
	}

	public void appendNodeHierarchy(String rootName, String groupName, String first, String second, ArrayList<JPair<Integer, String>> list, ArrayList<Boolean> selected) {
		appendNodeHierarchy(rootName, groupName, first, second, list, selected, true);
	}

	public void appendNodeHierarchyNoCheck(String rootName, String groupName, String first, String second, ArrayList<JPair<Integer, String>> list) {
		appendNodeHierarchy(rootName, groupName, first, second, list, null, false);
	}

	public void appendNodeHierarchyNoCheck(String rootName, String groupName, String first, String second, ArrayList<JPair<Integer, String>> list, ArrayList<Boolean> selected) {
		appendNodeHierarchy(rootName, groupName, first, second, list, selected, false);
	}

	private void appendNodeHierarchy(String rootName, String groupName, String first, String second, ArrayList<JPair<Integer, String>> list, ArrayList<Boolean> selected, boolean emptyCheck) {
		String r = JUtility.valueOf(rootName, FIRST).trim();
		if (r.length() > 0) {
			Element rt = doc.createElement(r);
			if (list.size() > 0) {
				for (int i = 0; i < list.size(); ++i) {
					JPair<Integer, String> pair = list.get(i);
					int id = pair.first;
					String name = JUtility.valueOf(pair.second, "").trim();
					String s = (selected != null && i < selected.size() ? Boolean.toString(selected.get(i)) : "");
					if (emptyCheck == false || name.length() > 0) {
						Element group = doc.createElement(JUtility.valueOf(groupName, SECOND).trim());
						if (s.length() > 0)
							group.setAttribute(SELECTED, s);
						Element elem = doc.createElement(JUtility.valueOf(first, KEY).trim());
						elem.appendChild(doc.createTextNode(JUtility.valueOf(Integer.toString(id), "")));
						group.appendChild(elem);
						elem = doc.createElement(JUtility.valueOf(second, VALUE).trim());
						elem.appendChild(doc.createTextNode(JUtility.valueOf(name, "")));
						group.appendChild(elem);
						rt.appendChild(group);
					}
				}
			}
			if (emptyCheck == false || list.size() > 0)
				root.appendChild(rt);
		}
	}

	public void copyFrom(JDocument source) {
		NodeList children = source.doc.getDocumentElement().getChildNodes();
		int count = children.getLength();
		for (int i = 0; i < count; ++i) {
			Node child = children.item(i);
			Node clone = doc.importNode(child, true);
			root.appendChild(clone);
		}
	}

	public void writeToStream(OutputStream out) throws IOException, SAXException {
		writeToStreamInternal(null, out);
	}

	public void writeToStream(String stylesheet, OutputStream out) throws IOException, SAXException {
		writeToStreamInternal(stylesheet, out);
	}

	private void writeToStreamInternal(String stylesheet, OutputStream out) throws IOException, SAXException {
		OutputFormat of = new OutputFormat("XML", Formatter.DEFAULT, true);
		of.setIndent(1);
		of.setIndenting(true);
		XMLSerializer serializer = new XMLSerializer(out, of);
		serializer.asDOMSerializer();
		if (stylesheet != null)
			serializer.processingInstruction("xml-stylesheet", "type=\"" + Formatter.XSL + "\" href=\"" + stylesheet + "\"");
		serializer.serialize(doc.getDocumentElement());		
	}
}
