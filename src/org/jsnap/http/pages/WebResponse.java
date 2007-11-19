package org.jsnap.http.pages;

import java.io.IOException;
import java.io.OutputStream;

import org.jsnap.http.base.HttpResponse;
import org.jsnap.util.JDocument;
import org.xml.sax.SAXException;

public class WebResponse {
	public final JDocument body;
	public final String stylesheet;
	public final String contentType;
	public final String characterSet;

	public WebResponse(JDocument body, String stylesheet, String contentType, String characterSet) {
		this.body = body;
		this.stylesheet = stylesheet;
		this.contentType = contentType;
		this.characterSet = characterSet;
	}

	public void writeToStream(OutputStream out) throws IOException, SAXException {
		body.writeToStream(stylesheet, out);
	}

	public void writeToHttpResponse(HttpResponse response) throws IOException, SAXException {
		writeToStream(response.out);
		response.contentType = contentType;
		response.characterSet = characterSet;
	}
}
