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
