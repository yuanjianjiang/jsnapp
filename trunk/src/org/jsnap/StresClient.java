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
import org.jsnap.request.HttpRequest;
import org.jsnap.request.SSLRequest;
import org.jsnap.request.SizePackedSecureRequest;
import org.jsnap.request.SizePackedRequest;
import org.jsnap.response.Formatter;
import org.jsnap.response.Response;
import org.jsnap.security.Credentials;

public class StresClient {
	private static final int THREAD_COUNT = 10;
	private static final int MILLIS_TO_RUN_FOR = 60000;

	public static void main(String[] args) {
		String requestClass = SizePackedRequest.class.getCanonicalName();
		if (args.length > 0)
			requestClass = args[0];
		String scenario = "execute";
		if (args.length > 1)
			scenario = args[1];
		try {
			Class.forName(requestClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			requestClass = SizePackedRequest.class.getCanonicalName();
		}
		if (scenario.equals("execute") == false && scenario.equals("reserve") == false)
			scenario = "execute";
		System.out.println(requestClass);
		System.out.println(scenario);
		for (int i = 0; i < THREAD_COUNT; ++i)
			new StresThread(requestClass, scenario, MILLIS_TO_RUN_FOR).start();
	}

	private static class StresThread extends Thread {
		private final long until;
		private final int r;
		private final int s;
		private long count;

		public StresThread(String requestClass, String scenario, long period) {
			until = System.currentTimeMillis() + period;
			if (requestClass.equals(SizePackedRequest.class.getCanonicalName()))
				r = 0;
			else if (requestClass.equals(HttpRequest.class.getCanonicalName()))
				r = 1;
			else if (requestClass.equals(SizePackedSecureRequest.class.getCanonicalName()))
				r = 2;
			else if (requestClass.equals(SSLRequest.class.getCanonicalName()))
				r = 3;
			else
				r = -1;
			if (scenario.equals("execute"))
				s = 0;
			else if (scenario.equals("reserve"))
				s = 1;
			else
				s = -1;
			count = 0;
		}

		public void run() {
			long now = 0;
			while (now < until) {
				try {
					DbRequest req = null;
					switch (r) {
					case 0: req = new SizePackedRequest(IPADDRESS, 52660); break;
					case 1: req = new HttpRequest(IPADDRESS, 52661); break;
					case 2: req = new SizePackedSecureRequest(IPADDRESS, 52662, true); break;
					case 3: req = new SSLRequest(IPADDRESS, 52663, true); break;
					}
					switch (s) {
					case 0:
						execute(req, 0, 0);
						++count;
						break;
					case 1:
						int key = reserve(req);
						access(req, key, 0, 0);
						close(req, key, true);
						++count;
						break;
					}
				} catch (JSnapException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				now = System.currentTimeMillis();
			}
			System.out.println(count);
		}

		private static final String IPADDRESS = "192.168.1.101";
		private static final String DATABASE = "postgres";
		private static final String FORMATTER = "org.jsnap.response.CSVFormatter?header=true";
		//private static final String FORMATTER = "org.jsnap.response.HTMLFormatter?css=https://localhost:8080/style.css&home=https://localhost:8080/SqlExecutor.html&perpage=10";
		//private static final String FORMATTER = "org.jsnap.response.XMLFormatter?metadata=false&resultset=query&record=row&nocaps=true";
		private static final long TIMEOUT = 0;
		private static final long KEEPALIVE = 60000;
		private static final int ZIP = 0;
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
			req.receive();
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
			return f.extractKey(response);
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
			req.receive();
		}

		private static void close(DbRequest req, int key, boolean commit) throws JSnapException {
			req.clear();
			req.setCommand(commit ? "commit" : "rollback");
			req.setTimeout(TIMEOUT);
			req.setZip(ZIP);
			req.setKey(key);
			req.setCredentials(new Credentials(USER, PASSWORD));
			req.send();
			req.receive();
		}
	}
}
