package org.jsnap;

import java.io.File;

import org.apache.log4j.xml.DOMConfigurator;
import org.jsnap.exception.DefaultExceptionHandler;
import org.jsnap.util.JThread;

public final class Launcher {
	private static long LOGGER_WATCH_PERIOD = 10000; // 10 seconds.

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Fatal: Expected from command line: <log4j.xml> <server.xml>");
			System.exit(-1);
		}
		String log4jFile = args[0];
		String serverFile = args[1];

		if (new File(log4jFile).exists() == false) {
			System.err.println("Fatal: " + log4jFile + " does not exist");
			System.exit(-2);
		}
		DOMConfigurator.configureAndWatch(log4jFile, LOGGER_WATCH_PERIOD);

		Thread.setDefaultUncaughtExceptionHandler(DefaultExceptionHandler.INSTANCE);
		Thread.currentThread().setName("Launcher");

		// Creates and configures the database registry, the listener container
		// and the pool of worker threads. All threads except the configuration
		// watchdog thread are daemon threads, only the configuration watchdog
		// keeps the JVM alive after this Launcher thread terminates.
		ConfigurationWatchdog watchdogRunnable = new ConfigurationWatchdog(serverFile);
		Thread watchdogThread = JThread.newThread("CfgWatchdog", watchdogRunnable);
		watchdogThread.start();

		Runtime.getRuntime().addShutdownHook(new ShutdownHook(watchdogRunnable));
	}
}
