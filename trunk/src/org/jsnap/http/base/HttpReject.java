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

package org.jsnap.http.base;

import java.io.PrintWriter;

import org.apache.http.HttpStatus;
import org.jsnap.response.Formatter;

public class HttpReject extends HttpServlet {
	public HttpReject(HttpServerConnection c) {
		super(c);
	}

	protected void doServiceImpl(HttpRequest request, HttpResponse response) {
		response.statusCode = HttpStatus.SC_SERVICE_UNAVAILABLE;
		response.contentType = Formatter.PLAIN;
		response.characterSet = Formatter.ISO_8859_1;
		PrintWriter writer = new PrintWriter(response.out);
		writer.println("Server is too busy to handle your request. Please retry at a later time.");
	}
}
