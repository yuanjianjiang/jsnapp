package org.jsnap.http.pages;

import org.jsnap.util.JDocument;
import org.jsnap.util.JUtility;

public abstract class AbstractWebPage implements WebPage {
	public void appendDefaults(JDocument doc) {
		doc.appendTextNode("key", key());
		doc.appendTextNode("now", JUtility.toString(System.currentTimeMillis()));
	}
}
