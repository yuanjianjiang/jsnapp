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

package org.jsnap.db.base;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.http.pages.AbstractWebPage;
import org.jsnap.http.pages.WebPage;
import org.jsnap.http.pages.WebResponse;
import org.jsnap.response.Formatter;
import org.jsnap.util.JDocument;
import org.jsnap.util.JThread;
import org.jsnap.util.JUtility;

public final class DbInstanceTracker implements Runnable {
	public static class LongComparator implements Comparator<Long> {
		public static LongComparator INSTANCE = new LongComparator(); 

		public int compare(Long o1, Long o2) {
			if (o1 < o2)
				return -1;
			else if (o1 > o2)
				return 1;
			return 0;
		}
	}

	private final Lock mutex;
	private SortedSet<Long> timepoints;
	private final HashMap<Long, Set<DbInstance>> forward;
	private final HashMap<DbInstance, Long> reverse;
	private Object pillow = new Object();
	private final Thread executingThread;
	private long nextWakeUp;
	private final WebPage page;

	private static final long MINIMUM_SLEEP_PERIOD = 1000; // 1 second.
	private static final long SLEEP_TIL_INTERRUPTED = 0;


	public DbInstanceTracker() {
		timepoints = new TreeSet<Long>(LongComparator.INSTANCE);
		forward = new HashMap<Long, Set<DbInstance>>();
		reverse = new HashMap<DbInstance, Long>();
		mutex = new ReentrantLock(true); // fair.
		executingThread = JThread.newDaemonThread("DbiTracker", this);
		executingThread.setPriority(Thread.MAX_PRIORITY);
		executingThread.start();
		nextWakeUp = Long.MAX_VALUE;
		page = new DbInstanceTrackerPage();
	}

	public WebPage getPage() {
		return page;
	}

	public void disconnectIfIdleAt(DbInstance dbi, long timepoint) {
		mutex.lock();
		try {
			removeInstance(dbi);
			timepoints.add(timepoint);
			Set<DbInstance> s = forward.get(timepoint);
			if (s == null) {
				s = new HashSet<DbInstance>();
				forward.put(timepoint, s);
			}
			s.add(dbi);
			reverse.put(dbi, timepoint);
			// Wake up the executing thread if necessary.
			synchronized (pillow) { 				 // Need to lock pillow to access nextWakeUp.
				if (timepoint < nextWakeUp) { 		 // This database instance becomes idle sooner
					long d = nextWakeUp - timepoint; // than the executing thread's next wake up;
					if (d >= MINIMUM_SLEEP_PERIOD)	 // interrupt the executing thread so that
						executingThread.interrupt(); // it updates its internals.
				}
			}
		} finally {
			mutex.unlock();
		}
	}

	private void removeInstance(DbInstance dbi) {
		Long previous = reverse.get(dbi);
		if (previous != null) {
			Set<DbInstance> s = forward.get(previous);
			s.remove(dbi);
			if (s.isEmpty()) {
				timepoints.remove(previous);
				forward.remove(previous);
			}
			reverse.remove(dbi);
		}
	}

	public void run() {
		Logger logger = Logger.getLogger(DbInstanceTracker.class);
		logger.log(Level.INFO, "Up and running");
		while (true) {
			long sleep = SLEEP_TIL_INTERRUPTED;
			logger.log(Level.DEBUG, "Woke up");
			mutex.lock();
			try {
				if (timepoints.size() > 0) { // Leave immediately when no timepoints exist.
					long cutoff = System.currentTimeMillis();
					SortedSet<Long> idle = new TreeSet<Long>(timepoints.headSet(cutoff));
					for (long timepoint: idle) {
						Set<DbInstance> f = forward.get(timepoint);
						Set<DbInstance> s = new HashSet<DbInstance>(f);
						for (DbInstance dbi: s) {
							synchronized (dbi) {
								long lastActive = dbi.getLastActive();
								if (lastActive < timepoint)
									dbi.disconnect(true);
							}
							removeInstance(dbi);
						}
					}
					timepoints = timepoints.tailSet(cutoff);
					if (timepoints.size() > 0) {
						sleep = timepoints.first() - cutoff;
						if (sleep < MINIMUM_SLEEP_PERIOD)
							sleep = MINIMUM_SLEEP_PERIOD;
					}
				}
			} finally {
				mutex.unlock();
			}
			logger.log(Level.DEBUG, "Done, calling wait(" + sleep + ")");
			synchronized (pillow) {
				try {
					nextWakeUp = (sleep == SLEEP_TIL_INTERRUPTED ? Long.MAX_VALUE : System.currentTimeMillis() + sleep);
					pillow.wait(sleep);
				} catch (InterruptedException leave) {
				}
			}
		}
	}

	public final class DbInstanceTrackerPage extends AbstractWebPage {
		public String key() {
			return "info-dbitracker";
		}

		public String name() {
			return "Database Connections";
		}

		public String category() {
			return WebPage.CATEGORY_INFORMATION;
		}

		public String stylesheet() {
			return "/jwc/dbitracker.xsl";
		}

		public boolean administrative() {
			return false;
		}

		public WebResponse data(HttpRequest request) {
			JDocument doc = new JDocument("dbitracker");
			HashMap<DbInstance, Long> copy;
			mutex.lock();
			try {
				copy = new HashMap<DbInstance, Long>(reverse);
			} finally {
				mutex.unlock();
			}
			appendDefaults(doc);
			String[] names = new String[]{"database", "until"};
			Set<DbInstance> instances = copy.keySet();
			for (DbInstance dbi: instances) {
				String[] data = new String[2];
				data[0] = dbi.getOwnerName(); // database.
				long disconnectAtMillis = copy.get(dbi);
				data[1] = JUtility.toString(disconnectAtMillis); // until.
				doc.appendNodeHierarchy("instance", names, data);
			}
			return new WebResponse(doc, stylesheet(), Formatter.XML, Formatter.DEFAULT);
		}
	}
}
