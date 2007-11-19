package org.jsnap;

import org.jsnap.exception.DefaultExceptionHandler;

public final class ShutdownHook extends Thread {
	private ConfigurationWatchdog watchdog;

	public ShutdownHook(ConfigurationWatchdog watchdog) {
		setName("ShutdownHook");
		setUncaughtExceptionHandler(DefaultExceptionHandler.INSTANCE);
		this.watchdog = watchdog;
	}

	public void run() {
		watchdog.terminate();
	}
}
