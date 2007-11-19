package org.jsnap.http;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.db.ConnectException;
import org.jsnap.exception.db.SqlException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.PasswordManagementException;
import org.jsnap.exception.security.PasswordRenewalException;
import org.jsnap.exception.security.PasswordManagementException.Reason;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.http.base.HttpResponse;
import org.jsnap.http.base.HttpServerConnection;
import org.jsnap.http.base.HttpServlet;
import org.jsnap.http.base.HttpServletRunner;
import org.jsnap.http.base.HttpResponse.Cookie;
import org.jsnap.http.pages.WebPage;
import org.jsnap.request.Request;
import org.jsnap.response.Formatter;
import org.jsnap.security.AuthenticationPolicy.User;
import org.jsnap.security.AuthenticationPolicy.UserType;
import org.jsnap.server.Workers.Worker;
import org.jsnap.util.JDocument;
import org.jsnap.util.JUtility;
import org.xml.sax.SAXException;

public class WebConsoleRequest extends HttpServletRunner {
	protected Request create(HttpServerConnection connection) {
		WebConsoleRequest request = new WebConsoleRequest();
		request.servlet = new WebConsoleServlet(connection);
		return request;
	}

	public boolean secure() {
		return true;
	}

	private static class WebConsoleServlet extends HttpServlet {
		// DEV: Implement a method for registering and retrieving users' files.
		private static final String TMP_ROOT = "tmp";
		private static final String WEB_ROOT = "webroot";
		private static final int BUFFER_SIZE = 512; // 512 bytes.
		private static final long LOGIN_LASTS_FOR = 180000; // 3 minutes.

		private static final String URI_ROOT = "/";
		private static final String URI_DIRECTORY_INDEX = "/index.xml";
		private static final String URI_PERCENTAGE = "/percentage";
		private static final String URI_LOGIN_SHOW = "/login.xml";
		private static final String URI_LOGIN_DO = "/login.do";
		private static final String URI_LOGIN_XSL = "/login.xsl";
		private static final String URI_LOGOUT_SHOW = "/jwc/logout.xml";
		private static final String URI_RENEW_SHOW = "/renew.xml";
		private static final String URI_RENEW_DO = "/renew.do";
		private static final String URI_RENEW_XSL = "/renew.xsl";
		private static final String URI_PATH_FOR_COOKIES = "/jwc";
		private static final String URI_MAIN_HTML = "/jwc/main.html";
		private static final String URI_MAIN_DO = "/jwc/main.do";
		private static final String URI_HEADER_XML = "/jwc/header.xml";
		private static final String URI_HEADER_XSL = "/jwc/header.xsl";

		private static final String COOKIE_USERNAME = "jsnapweb-username";
		private static final String COOKIE_ADMIN = "jsnapweb-admin";

		private static final String[] prefferedWriters = new String[]{"png", "gif", "jpg"};
		private static String preferredWriter = null;
		static {
			ImageWriter imageWriter = null;
			for (int i = 0; imageWriter == null && i < prefferedWriters.length; ++i) {
				Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(prefferedWriters[i]);
				if (writers.hasNext()) {
					imageWriter = writers.next();
					preferredWriter = prefferedWriters[i];
				}
			}
		}

		private static class Percentage {
			public final int percentage, width, height, border;
			public final Color bgcolor, fgcolor;

			private final String background, foreground;

			private static final int DEFAULT_PERCENTAGE = 0;
			private static final int DEFAULT_WIDTH = 100;
			private static final int DEFAULT_HEIGHT = 14;
			private static final int DEFAULT_BORDER = 1;
			private static final String DEFAULT_BACKGROUND = "000000";
			private static final String DEFAULT_FOREGROUND = "00c000";

			private static final HashSet<String> availableFontNames = new HashSet<String>();
			static {
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				String[] fonts = ge.getAvailableFontFamilyNames();
				for (String font: fonts)
					availableFontNames.add(font);
			}

			public Percentage(HttpRequest request) {
				int p = JUtility.valueOf(request.parameters.get("p"), -1);
				percentage = (p <= 0 ? DEFAULT_PERCENTAGE : p);
				int w = JUtility.valueOf(request.parameters.get("w"), -1);
				width = (w <= 0 ? DEFAULT_WIDTH : w);
				int h = JUtility.valueOf(request.parameters.get("h"), -1);
				height = (h <= 0 ? DEFAULT_HEIGHT : h);
				int b = JUtility.valueOf(request.parameters.get("b"), -1);
				border = (b < 0 ? DEFAULT_BORDER : b);
				Color c;
				String bg = request.parameters.get("bg");
				try {
					c = Color.decode("0x" + bg);
				} catch (Throwable e) {
					c = Color.decode("0x" + DEFAULT_BACKGROUND);
					bg = DEFAULT_BACKGROUND;
				}
				background = bg;
				bgcolor = c;
				String fg = request.parameters.get("fg");
				try {
					c = Color.decode("0x" + fg);
				} catch (Throwable e) {
					c = Color.decode("0x" + DEFAULT_FOREGROUND);
					fg = DEFAULT_FOREGROUND;
				}
				foreground = fg;
				fgcolor = c;
			}

			public String toString() {
				return background +
					   foreground +
					   "p" + Integer.toString(percentage) +
					   "w" + Integer.toString(width) +
					   "h" + Integer.toString(height) +
					   "b" + Integer.toString(border);
			}
		}

		private Worker executingThread;

		public WebConsoleServlet(HttpServerConnection c) {
			super(c);
			executingThread = (Worker)Thread.currentThread();
		}

		protected void doServiceImpl(HttpRequest request, HttpResponse response) {
			Logger.getLogger(WebConsoleServlet.class).log(Level.DEBUG, "Request URI is " + request.uri);
			try {
				// Displays the login form.
				if (request.uri.equals(URI_ROOT) || request.uri.equals(URI_LOGIN_SHOW)) {
					showLogin(response, null, null, null);
				// Process login request. (Authentication takes place.)
				} else if (request.uri.equals(URI_LOGIN_DO)) {
					doLogin(request, response);
				// Process login request. (Authentication takes place.)
				} else if (request.uri.equals(URI_LOGOUT_SHOW)) {
					doLogout(request, response);
				// Displays the password renewal form.
				} else if (request.uri.equals(URI_RENEW_SHOW)) {
					showRenew(response, null, null, null, null, null, false);
				// Process password renewal request.
				} else if (request.uri.equals(URI_RENEW_DO)) {
					doRenew(request, response);
				// Displays main menu.
				} else if (request.uri.equals(URI_MAIN_HTML)) {
					showMain(request, response);
				// Displays header frame.
				} else if (request.uri.equals(URI_HEADER_XML)) {
					showHeader(request, response);
				// Displays main frame.
				} else if (request.uri.equals(URI_MAIN_DO)) {
					doMain(request, response);
				// Send (first create if necessary) a dynamic percentage image.
				} else if (request.uri.equals(URI_PERCENTAGE)) {
					Percentage percentage = new Percentage(request);
					File f = new File(TMP_ROOT + URI_ROOT + percentage.toString() + "." + preferredWriter);
					if (f.exists() == false)
						createImage(percentage);
					sendFile(f, response);
				// Send a file that exists under WEB_ROOT.
				} else {
					File f = new File(WEB_ROOT + request.uri);
					if (f.isDirectory()) {
						File i = new File(WEB_ROOT + request.uri + URI_DIRECTORY_INDEX);
						if (i.exists())
							f = i;
					}
					sendFile(f, response);
				}
			} catch (Throwable t) {
				PrintWriter writer = new PrintWriter(response.out);
				t.printStackTrace(writer);
				writer.flush();
				response.contentType = Formatter.PLAIN;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
			}
			if (response.contentType.startsWith(Formatter.TEXT)) {
				response.zipSize = 1; // Output is gzipped only for text output.
			} else {
				response.zipSize = 0; // Output will not get gzipped.
			}
		}

		private void showLogin(HttpResponse response, String username, String password, String error) throws IOException, SAXException {
			JDocument doc = new JDocument("login");
			doc.appendTextNode("username", username);
			doc.appendTextNode("password", password);
			doc.appendTextNode("error", error);
			doc.writeToStream(URI_LOGIN_XSL, response.out);
			response.contentType = Formatter.XML;
			response.characterSet = Formatter.DEFAULT;
			response.statusCode = HttpStatus.SC_OK;
		}

		private void doLogin(HttpRequest request, HttpResponse response) throws LoginFailedException, IOException, SAXException {
			long tryUntil = System.currentTimeMillis() + WEB_DB_TIMEOUT;
			LoginFailedException failed = null;
			User user = null;
			DbInstance dbi = null;
			try {
				long remaining = tryUntil - System.currentTimeMillis();
				user = executingThread.authenticateWeb(request.credentials, remaining);
			} catch (ConnectException e) {
				failed = new LoginFailedException(e);
			} catch (LoginFailedException e) {
				failed = e;
			} finally {
				if (dbi != null)
					dbi.close();
			}
			if (failed == null) { // Login successful.
				completeLogin(response, user);
			} else {
				if (failed.reason == Reason.EXCEPTION)
					throw failed;
				else if (failed.reason == Reason.RENEW_PASSWORD)
					showRenew(response, request.credentials.username, request.credentials.password, null, null, null, true);
				else
					showLogin(response, request.credentials.username, request.credentials.password, failed.getMessage());
			}
		}

		private void doLogout(HttpRequest request, HttpResponse response) throws IOException, SAXException {
			String username = request.cookies.get(COOKIE_USERNAME);
			String admin = request.cookies.get(COOKIE_ADMIN);
			if (username == null || admin == null) {
				showLogin(response, null, null, "Your session has timed out");
			} else {
				terminateSession(response, username, admin);
				showLogin(response, null, null, null);
			}
		}

		private void showRenew(HttpResponse response, String username, String password, String password1, String password2, String error, boolean forced) throws IOException, SAXException {
			JDocument doc = new JDocument("renew");
			doc.appendEmptyNode(forced ? "forced" : "requested");
			doc.appendTextNode("username", username);
			doc.appendTextNode("password", password);
			doc.appendTextNode("password1", password1);
			doc.appendTextNode("password2", password2);
			doc.appendTextNode("error", error);
			doc.writeToStream(URI_RENEW_XSL, response.out);
			response.contentType = Formatter.XML;
			response.characterSet = Formatter.DEFAULT;
			response.statusCode = HttpStatus.SC_OK;
		}

		private void doRenew(HttpRequest request, HttpResponse response) throws PasswordManagementException, IOException, SAXException {
			long tryUntil = System.currentTimeMillis() + WEB_DB_TIMEOUT;
			String password1 = JUtility.valueOf(request.parameters.get("password1"), "");
			String password2 = JUtility.valueOf(request.parameters.get("password2"), "");
			String reason = request.parameters.get("reason");
			if (password1.equals(password2) == false) {
				showRenew(response, request.credentials.username, request.credentials.password, null, null, new PasswordRenewalException("Supplied passwords do not match").getMessage(), reason.equals("forced"));
			} else {
				PasswordManagementException failed = null;
				User user = null;
				DbInstance dbi = null;
				try {
					long remaining = tryUntil - System.currentTimeMillis();
					user = executingThread.changePassword(request.credentials, password1, remaining);
				} catch (ConnectException e) {
					throw new PasswordRenewalException(e);
				} catch (PasswordManagementException e) {
					failed = e;
				} finally {
					if (dbi != null)
						dbi.close();
				}
				if (failed == null) { // Password successfully renewed.
					completeLogin(response, user);
				} else {
					if (failed.reason == Reason.EXCEPTION)
						throw failed;
					else
						showRenew(response, request.credentials.username, request.credentials.password, null, null, failed.getMessage(), reason.equals("forced"));
				}
			}
		}

		private void completeLogin(HttpResponse response, User user) {
			response.cookies.add(new Cookie(COOKIE_USERNAME, user.credentials.username, URI_PATH_FOR_COOKIES, LOGIN_LASTS_FOR));
			response.cookies.add(new Cookie(COOKIE_ADMIN, Boolean.toString(user.userType == UserType.ADMINISTRATOR), URI_PATH_FOR_COOKIES, LOGIN_LASTS_FOR));
			response.redirectTo = URI_MAIN_HTML;
		}

		private void showHeader(HttpRequest request, HttpResponse response) throws IOException, SAXException {
			String username = request.cookies.get(COOKIE_USERNAME);
			String admin = request.cookies.get(COOKIE_ADMIN);
			if (username == null || admin == null) {
				showLogin(response, null, null, "Your session has timed out");
			} else {
				JDocument pages = executingThread.getPageList(Boolean.valueOf(admin));
				pages.writeToStream(URI_HEADER_XSL, response.out);
				response.contentType = Formatter.XML;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_OK;
				// Refresh cookies' expiry dates so that the session does not time out.
				extendSession(response, username, admin);
			}
		}

		private void showMain(HttpRequest request, HttpResponse response) throws IOException, SAXException {
			String username = request.cookies.get(COOKIE_USERNAME);
			String admin = request.cookies.get(COOKIE_ADMIN);
			if (username == null || admin == null) {
				showLogin(response, null, null, "Your session has timed out");
			} else {
				PrintWriter writer = new PrintWriter(response.out);
				writer.println("<html>");
				writer.println("<head>");
				writer.println("  <link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\"/>");
				writer.println("  <title>JSnap Web Console</title>");
				writer.println("</head>");
				writer.println("<frameset rows=\"95,*\" frameborder=\"no\" framespacing=\"0\">");
				writer.println("  <frame name=\"top\" scrolling=\"no\" noresize src=\"header.xml\"/>");
				writer.println("  <frame name=\"bottom\" scrolling=\"auto\" src=\"main.do\"/>");
				writer.println("  <noframes>");
				writer.println("    <p>You need a browser that supports frames.</p>");
				writer.println("  </noframes>");
				writer.println("</frameset>");
				writer.println("</html>");
				writer.flush();
				response.contentType = Formatter.HTML;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_OK;
				// Refresh cookies' expiry dates so that the session does not time out.
				extendSession(response, username, admin);
			}
		}

		private void doMain(HttpRequest request, HttpResponse response) throws IOException, SAXException, SqlException {
			String username = request.cookies.get(COOKIE_USERNAME);
			String admin = request.cookies.get(COOKIE_ADMIN);
			if (username == null || admin == null) {
				showLogin(response, null, null, "Your session has timed out");
			} else {
				WebPage wp = null;
				String page = request.parameters.get("page");
				if (page != null)
					wp = executingThread.getPage(page);
				if (wp == null)
					wp = executingThread.getSummaryPage(Boolean.valueOf(admin));
				wp.data(request).writeToHttpResponse(response);
				response.statusCode = HttpStatus.SC_OK;
				// Refresh cookies' expiry dates so that the session does not time out.
				extendSession(response, username, admin);
			}
		}

		private void extendSession(HttpResponse response, String username, String admin) {
			response.cookies.add(new Cookie(COOKIE_USERNAME, username, URI_PATH_FOR_COOKIES, LOGIN_LASTS_FOR));
			response.cookies.add(new Cookie(COOKIE_ADMIN, admin, URI_PATH_FOR_COOKIES, LOGIN_LASTS_FOR));
		}	

		private void terminateSession(HttpResponse response, String username, String admin) {
			response.cookies.add(new Cookie(COOKIE_USERNAME, username, URI_PATH_FOR_COOKIES, 0));
			response.cookies.add(new Cookie(COOKIE_ADMIN, admin, URI_PATH_FOR_COOKIES, 0));
		}	

		private void createImage(Percentage p) throws IOException {
			if (preferredWriter != null) {
				int filled = p.width * p.percentage / 100;
				int whole = filled;
				BufferedImage image = new BufferedImage(whole, p.height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = image.createGraphics();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, whole, p.height);
				g.setColor(p.bgcolor);
				g.fillRect(0, 0, filled, p.height);
				g.setColor(p.fgcolor);
				g.fillRect(p.border, p.border, filled - 2 * p.border, p.height - 2 * p.border);
				g.setColor(p.bgcolor);
				File f = new File(TMP_ROOT + "/" + p.toString() + "." + preferredWriter);
				FileImageOutputStream fios = new FileImageOutputStream(f);
				Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(preferredWriter);
				if (writers.hasNext()) {
					ImageWriter writer = writers.next();
					writer.setOutput(fios);
					writer.write(image);
				} else {
					Logger.getLogger(WebConsoleServlet.class).log(Level.ERROR, "No image writers are available to create a dynamic image");
				}
			} else {
				Logger.getLogger(WebConsoleServlet.class).log(Level.ERROR, "No image writers are available to create a dynamic image");
			}
		}

		private void sendFile(File file, HttpResponse response) throws IOException {
			if (file.exists() && file.isFile()) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
				FileInputStream fis = new FileInputStream(file);
				try {
					int read;
					byte[] b = new byte[BUFFER_SIZE];
					while ((read = fis.read(b)) > 0)
						baos.write(b, 0, read);
					response.out.write(baos.toByteArray());
					int ix = file.getName().lastIndexOf(".");
					if (ix < 0) {
						response.contentType = Formatter.BINARY;
					} else {
						String suffix = file.getName().substring(ix + 1);
						if (suffix.equals("txt")) {
							response.contentType = Formatter.PLAIN;
							response.characterSet = Formatter.DEFAULT;
						} else if (suffix.equals("htm") || suffix.equals("html")) {
							response.contentType = Formatter.HTML;
							response.characterSet = Formatter.DEFAULT;
						} else if (suffix.equals("css")) {
							response.contentType = Formatter.CSS;
							response.characterSet = Formatter.DEFAULT;
						} else if (suffix.equals("xsl")) {
							response.contentType = Formatter.XSL;
							response.characterSet = Formatter.DEFAULT;
						} else if (suffix.equals("xml")) {
							response.contentType = Formatter.XML;
							response.characterSet = Formatter.DEFAULT;
						} else if (suffix.equals("jpg")) {
							response.contentType = Formatter.JPEG;
						} else if (suffix.equals("gif")) {
							response.contentType = Formatter.GIF;
						} else if (suffix.equals("png")) {
							response.contentType = Formatter.PNG;
						} else {
							response.contentType = Formatter.BINARY;
						}
					}
					response.statusCode = HttpStatus.SC_OK;
				} finally {
					fis.close();
				}
			} else {
				PrintWriter writer = new PrintWriter(response.out);
				writer.println("Requested location does not exist.");
				writer.flush();
				response.contentType = Formatter.PLAIN;
				response.characterSet = Formatter.DEFAULT;
				response.statusCode = HttpStatus.SC_NOT_FOUND;
			}		
		}
	}
}
