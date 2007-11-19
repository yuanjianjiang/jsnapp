package org.jsnap.exception;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public final class DefaultExceptionHandler implements UncaughtExceptionHandler {
	public static final DefaultExceptionHandler INSTANCE = new DefaultExceptionHandler();

	public void uncaughtException(Thread t, Throwable e) {
		Logger.getLogger(DefaultExceptionHandler.class).log(Level.FATAL, "Terminated because of unhandled throwable", e);
	}
}
