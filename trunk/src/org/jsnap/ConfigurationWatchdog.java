package org.jsnap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.jsnap.db.base.Dbregistry;
import org.jsnap.db.base.Database.DbProperties;
import org.jsnap.exception.security.KeyStoreInitiateException;
import org.jsnap.exception.security.PolicyException;
import org.jsnap.exception.server.ListenerCreateException;
import org.jsnap.exception.server.ServerConfigureException;
import org.jsnap.http.WebPageContainer;
import org.jsnap.response.ResponseTracker;
import org.jsnap.security.AccessControlPolicy;
import org.jsnap.security.AllowAdministratorsOnly;
import org.jsnap.security.AllowAll;
import org.jsnap.security.AuthenticationPolicy;
import org.jsnap.security.KeyStore;
import org.jsnap.security.SecurityHelper;
import org.jsnap.server.Listener;
import org.jsnap.server.ListenerContainer;
import org.jsnap.server.Workers;
import org.jsnap.util.JThread;
import org.jsnap.util.JUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public final class ConfigurationWatchdog implements Runnable {
	// DEV: Implement the capability of running scheduled tasks.
	private final File file;
	private final Workers workers;
	private final KeyStore keyStore;
	private final Dbregistry dbregistry;
	private final ResponseTracker responseTracker;
	private final WebPageContainer webPageContainer;
	private final ListenerContainer listenerContainer;
	private ArrayList<DbProperties> databases;
	private ArrayList<Listener> listeners;
	private DbProperties[] databaseArray;
	private long confWatch, lastModified;
	private Listener[] listenerArray;
	private Thread executingThread;
	private boolean keyStoreIsSet;
	private boolean running;
	// This period is used only when the first configure() call fails
	// or a negative value is specified in the configuration file.
	private static final long DEFAULT_WATCH_PERIOD = 10; // seconds.

	public ConfigurationWatchdog(String filename) {
		executingThread = null;
		confWatch = DEFAULT_WATCH_PERIOD * 1000;
		lastModified = -1;
		file = new File(filename);
		keyStore = new KeyStore();
		dbregistry = new Dbregistry();
		responseTracker = new ResponseTracker();
		listenerContainer = new ListenerContainer();
		webPageContainer = new WebPageContainer(dbregistry, responseTracker, listenerContainer);
		workers = new Workers(dbregistry, responseTracker, webPageContainer);
		webPageContainer.setThreadPool(workers); // Thread pool information should also be displayed.
		databases = new ArrayList<DbProperties>();
		listeners = new ArrayList<Listener>();
		running = true;
	}

	public void run() {
		executingThread = Thread.currentThread();
		Logger.getLogger(ConfigurationWatchdog.class).log(Level.INFO, "Up and running");
		while (running) {
			long ini = System.currentTimeMillis();
			long current = file.lastModified();
			if (current > lastModified) { // reconfigure only if the file has been modified.
				try {
					configure();
					dbregistry.update(databaseArray);
					listenerContainer.update(listenerArray);
				} catch (ServerConfigureException e) {
					e.log(); // logged and skipped.
				} catch (Throwable t) {
					new ServerConfigureException(t).log(); // logged and skipped.
				}
				lastModified = current;
			}
			long fin = System.currentTimeMillis();
			long sleepFor = confWatch - fin + ini; // (watch period) - (time passed while configuring)
			JThread.sleepInterruptibly(sleepFor);
		}
		Logger.getLogger(ConfigurationWatchdog.class).log(Level.INFO, "Shutting down");
		// The order in which components are shutdown is critical.
		// Listeners and workers are shutdown first to make sure that no new clients
		// are served; once that is done, stored responses are cleared up. (Result sets
		// are closed, active transactions are rolled back etc.) Finally the database
		// connections are closed by shutting down the database registry.
		listenerContainer.shutdown();
		workers.shutdown();
		try {
			workers.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException ignore) {
			Logger.getLogger(ConfigurationWatchdog.class).log(Level.DEBUG, "Ignored an interrupt");
		}
		if (workers.isShutdown() == false)
			workers.shutdownNow();
		responseTracker.shutdown();
		dbregistry.shutdown();
		Logger.getLogger(ConfigurationWatchdog.class).log(Level.WARN, "Terminated");
		// JVM will terminate after this point since all other threads are daemon threads.
	}

	public void terminate() {
		running = false;
		if (executingThread != null) 
			executingThread.interrupt(); // Executing thread sleeps interruptibly.
	}

	private void configure() throws ServerConfigureException {
		Document document = null;
		try {
			FileReader reader = new FileReader(file);
			InputSource source = new InputSource(reader);
			DOMParser parser = new DOMParser();
			parser.setErrorHandler(parseErrorHandler);
			parser.setFeature("http://xml.org/sax/features/validation", true);
			parser.parse(source);
			document = parser.getDocument();
		} catch (FileNotFoundException e) {
			throw new ServerConfigureException(e);
		} catch (SAXException e) {
			throw new ServerConfigureException(e);
		} catch (IOException e) {
			throw new ServerConfigureException(e);
		}
		databases.clear();
		listeners.clear();
		keyStoreIsSet = false;
		trace(document.getDocumentElement());
		ArrayList<Listener> allowedListeners = new ArrayList<Listener>();
		if (keyStoreIsSet == false) {
			keyStore.clear();
			Logger.getLogger(ConfigurationWatchdog.class).log(Level.INFO, "Keystore has not been initiated");
			for (Listener listener: listeners) {
				if (listener.secure()) {
					Logger.getLogger(ConfigurationWatchdog.class).log(Level.WARN, "Keystore missing, secure listener won't be started");
				} else {
					allowedListeners.add(listener);
				}
			}
		} else {
			allowedListeners.addAll(listeners);
		}
		listeners.clear();
		listeners.addAll(allowedListeners);
		databaseArray = new DbProperties[databases.size()];
		listenerArray = new Listener[listeners.size()];
		databases.toArray(databaseArray);
		listeners.toArray(listenerArray);
	}

	private void trace(Node root) {
		// server.dtd assures that the xml document is correctly structured.
		boolean goDeeper = false;
		String nodeName = root.getNodeName();
		// <configuration>
		if (nodeName.equals("configuration")) {
			long w = JUtility.valueOf(root.getAttributes().getNamedItem("watchdog").getNodeValue(), -1);
			// Specified in seconds in xml file, stored in millis here.
			if (w <= 0)
				w = DEFAULT_WATCH_PERIOD;
			if ((w * 1000) != confWatch) { 
				confWatch = w * 1000;
				Logger.getLogger(ConfigurationWatchdog.class).log(Level.DEBUG, "Configuration watch period is " + w + (w == 1 ? " second" : " seconds"));
			}
			goDeeper = true;
		// <internal>
		} else if (nodeName.equals("internal")) {
			HashMap<String, String> map = xmlToMap(root);
			String url = map.get("url");
			String driver = map.get("driver");
			int pool = JUtility.valueOf(map.get("pool"), -1);
			long idleTimeout = JUtility.valueOf(map.get("idle"), -1L);
			AuthenticationPolicy allowAdmins = new AllowAdministratorsOnly();
			allowAdmins.setOwnerName(Dbregistry.INTERNALDB_NAME);
			AccessControlPolicy allowAllPolicy = new AllowAll();
			allowAllPolicy.setOwnerName(Dbregistry.INTERNALDB_NAME);
			dbregistry.update(new DbProperties(Dbregistry.INTERNALDB_NAME, driver, url, pool, idleTimeout, 0, 0, 0, allowAdmins, allowAllPolicy));
		// <databases>
		} else if (nodeName.equals("databases")) {
			goDeeper = true;
		// <database>
		} else if (nodeName.equals("database")) {
			String name = root.getAttributes().getNamedItem("name").getNodeValue();
			HashMap<String, String> map = xmlToMap(root);
			String url = map.get("url");
			String driver = map.get("driver");
			int pool = JUtility.valueOf(map.get("pool"), -1);
			long idleTimeout = JUtility.valueOf(map.get("idle"), -1L);
			long initial = JUtility.valueOf(map.get("initial"), -1L);
			long increment = JUtility.valueOf(map.get("increment"), -1L);
			double multiplier = JUtility.valueOf(map.get("multiplier"), -1.0);
			String login = map.get("login");
			String accessControl = map.get("accesscontrol");
			try {
				AuthenticationPolicy loginP = SecurityHelper.getAuthenticationPolicy(name, login);
				AccessControlPolicy accessControlP = SecurityHelper.getAccessControlPolicy(name, accessControl);
				DbProperties prop = new DbProperties(name, driver, url, pool, idleTimeout, initial, increment, multiplier, loginP, accessControlP);
				databases.add(prop);
			} catch (PolicyException e) {
				e.log(); // logged and skipped.
			}
		// <listeners>
		} else if (nodeName.equals("listeners")) {
			goDeeper = true;
		// <listener>
		} else if (nodeName.equals("listener")) {
			HashMap<String, String> map = xmlToMap(root);
			int port = JUtility.valueOf(map.get("port"), (int)-1);
			int backlog = JUtility.valueOf(map.get("backlog"), (int)-1);
			String accepts = map.get("accepts");
			try {
				listeners.add(Listener.create(port, backlog, accepts, workers));
			} catch (ListenerCreateException e) {
				e.log(); // logged and skipped.
			}
		// <workers>
		} else if (nodeName.equals("workers")) {
			HashMap<String, String> map = xmlToMap(root);
			workers.setCorePoolSize(JUtility.valueOf(map.get("core"), -1));
			workers.setMaximumPoolSize(JUtility.valueOf(map.get("max"), -1));
			workers.setKeepAliveTime(JUtility.valueOf(map.get("idle"), -1L));
		// <keystore>
		} else if (nodeName.equals("keystore")) {
			HashMap<String, String> map = xmlToMap(root);
			String url = map.get("url");
			String password = map.get("password");
			if (url.length() > 0) {
				try {
					keyStore.load(url, password);
					keyStoreIsSet = true;
				} catch (KeyStoreInitiateException e) {
					e.log(); // logged and skipped.
				}
			}
		}
		// Trace document into deeper nodes.
		if (goDeeper) {
			NodeList children = root.getChildNodes();
			int childCount = children.getLength();
			for (int i = 0; i < childCount; ++i) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE)
					trace(child);
			}
		}
	}

	private HashMap<String, String> xmlToMap(Node root) {
		HashMap<String, String> map = new HashMap<String, String>();
		NodeList children = root.getChildNodes();
		int childCount = children.getLength();
		for (int i = 0; i < childCount; ++i) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				String value = child.getTextContent();
				map.put(name, value);
			}
		}
		return map;
	}

	private static final class ParseErrorHandler implements ErrorHandler {
		public void error(SAXParseException x) throws SAXException {
			throw x;
		}

		public void fatalError(SAXParseException x) throws SAXException {
			throw x;
		}

		public void warning(SAXParseException x) throws SAXException {
			Logger.getLogger(ConfigurationWatchdog.class).log(Level.WARN, "Received a warning while parsing server configuration file: " + x.getMessage());
		}
	}

	private final ParseErrorHandler parseErrorHandler = new ParseErrorHandler();
}
