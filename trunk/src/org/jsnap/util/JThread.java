package org.jsnap.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.exception.DefaultExceptionHandler;

public final class JThread {
	public static Thread newThread(String name) {
		Thread thread = new Thread();
		thread.setName(name);
		thread.setUncaughtExceptionHandler(DefaultExceptionHandler.INSTANCE);
		return thread;
	}

	public static Thread newThread(String name, Runnable r) {
		Thread thread = new Thread(r);
		thread.setName(name);
		thread.setUncaughtExceptionHandler(DefaultExceptionHandler.INSTANCE);
		return thread;
	}

	public static Thread newDaemonThread(String name) {
		Thread thread = newThread(name);
		thread.setDaemon(true);
		return thread;
	}

	public static Thread newDaemonThread(String name, Runnable r) {
		Thread thread = newThread(name, r);
		thread.setDaemon(true);
		return thread;
	}

	public static void sleep(long millis) {
		if (millis > 0) {
			long now = System.currentTimeMillis();
			long sleepUntil = now + millis;
			while (now < sleepUntil) {
				long sleepFor = sleepUntil - now;
				try {
					Thread.sleep(sleepFor);
				} catch (InterruptedException ignore) {
					Logger.getLogger(JThread.class).log(Level.DEBUG, "Ignored an interrupt");
				}
				now = System.currentTimeMillis();
			}
		}
	}

	public static void sleepInterruptibly(long millis) {
		try {
			if (millis > 0)
				Thread.sleep(millis);
		} catch (InterruptedException leave) {
		}
	}
}
