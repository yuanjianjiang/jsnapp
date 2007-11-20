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
