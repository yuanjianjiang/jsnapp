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

package org.jsnap;

import java.io.IOException;

import org.jsnap.exception.JSnapException;
import org.jsnap.request.DbRequest;
import org.jsnap.request.SizePackedRequest;
import org.jsnap.response.Formatter;
import org.jsnap.response.Response;
import org.jsnap.security.Credentials;

public class SimpleClient {
	public static void main(String[] args) throws JSnapException, IOException {
		DbRequest req = new SizePackedRequest("localhost", 52660);
		//DbRequest req = new HttpRequest("localhost", 52661);
		//DbRequest req = new SizePackedSecureRequest("localhost", 52662, true);
		//DbRequest req = new SSLRequest("localhost", 52663, true);
		execute(req, 0, 0);
		int key = reserve(req);
		access(req, key, 0, 0);
		close(req, key, true);
		
	}

	private static final String DATABASE = "postgres";
	private static final String FORMATTER = "org.jsnap.response.CSVFormatter?header=true";
	//private static final String FORMATTER = "org.jsnap.response.HTMLFormatter?css=https://localhost:8080/style.css&home=https://localhost:8080/SqlExecutor.html&perpage=10";
	//private static final String FORMATTER = "org.jsnap.response.XMLFormatter?metadata=false&resultset=query&record=row&nocaps=true";
	private static final long TIMEOUT = 3000;
	private static final long KEEPALIVE = 60000;
	private static final int ZIP = 1;
	private static final String USER = "admin";
	private static final String PASSWORD = "password";

	private static void setSql(DbRequest req) {
		req.setSql("SELECT 1");
		//req.setSql("SELECT * FROM authhistory WHERE username = ? AND result = ? ORDER BY timestamp");
		//req.addParameter(new DbParam(DbParam.STRING, "admin", false));
		//req.addParameter(new DbParam(DbParam.INTEGER, "0", false));
	}

	private static void execute(DbRequest req, int from, int to) throws JSnapException {
		req.clear();
		req.setCommand("execute");
		req.setDatabase(DATABASE);
		req.setFormatter(FORMATTER);
		setSql(req);
		req.setTimeout(TIMEOUT);
		req.setZip(ZIP);
		req.setFrom(from);
		req.setTo(to);
		req.setMaxRows(10);
		req.setCredentials(new Credentials(USER, PASSWORD));
		req.send();
		byte[] response = req.receive();
		System.out.println("EXECUTE:");
		System.out.println("---------");
		System.out.println("RESPONSE BODY:");
		System.out.println(new String(response));
	}

	private static int reserve(DbRequest req) throws JSnapException {
		req.clear();
		req.setCommand("reserve");
		req.setDatabase(DATABASE);
		req.setFormatter(FORMATTER);
		setSql(req);
		req.setTimeout(TIMEOUT);
		req.setKeepalive(KEEPALIVE);
		req.setZip(ZIP);
		req.setMaxRows(10);
		req.setCredentials(new Credentials(USER, PASSWORD));
		req.send();
		byte[] response = req.receive();
		Formatter f = Response.getFormatter(FORMATTER);
		int key = f.extractKey(response);
		System.out.println("RESERVE:");
		System.out.println("---------");
		System.out.println("RESPONSE BODY:");
		System.out.println(new String(response));
		System.out.println("ACCESS KEY:");
		System.out.println(key);
		return key;
	}

	private static void access(DbRequest req, int key, int from, int to) throws JSnapException {
		req.clear();
		req.setCommand("access");
		req.setTimeout(TIMEOUT);
		req.setZip(ZIP);
		req.setKey(key);
		req.setFrom(from);
		req.setTo(to);
		req.setCredentials(new Credentials(USER, PASSWORD));
		req.send();
		byte[] response = req.receive();
		System.out.println("ACCESS:");
		System.out.println("---------");
		System.out.println("RESPONSE BODY:");
		System.out.println(new String(response));
	}

	private static void close(DbRequest req, int key, boolean commit) throws JSnapException {
		req.clear();
		req.setCommand(commit ? "commit" : "rollback");
		req.setTimeout(TIMEOUT);
		req.setZip(ZIP);
		req.setKey(key);
		req.setCredentials(new Credentials(USER, PASSWORD));
		req.send();
		byte[] response = req.receive();
		System.out.println(commit ? "COMMIT:" : "ROLLBACK:");
		System.out.println("---------");
		System.out.println("RESPONSE BODY:");
		System.out.println(new String(response));
	}
}
