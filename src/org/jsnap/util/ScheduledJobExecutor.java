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

public abstract class ScheduledJobExecutor implements Runnable {
	public static abstract class ScheduledJob<K> implements Runnable {
		protected static final long IGNORED_TIMEPOINT = 0;

		public final long timepoint;
		public final K object;

		public ScheduledJob(long timepoint, K object) {
			this.timepoint = timepoint;
			this.object = object;
		}

		// Subclasses should override if necessary.
		public int hashCode() {
			return object.hashCode();
		}

		// Owned objects must be the same instance. Subclasses should override if necessary.
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof ScheduledJob) {
				ScheduledJob job = (ScheduledJob)obj;
				return (object == job.object); // True if and only if two jobs refer to the same object.
			}
			return false;
		}
	}

	private static class LongComparator implements Comparator<Long> {
		public static LongComparator INSTANCE = new LongComparator(); 

		public int compare(Long o1, Long o2) {
			if (o1 < o2)
				return -1;
			else if (o1 > o2)
				return 1;
			return 0;
		}
	}

	// If a subclass respects fail fast behaviour of iterating through
	// the structures timepoints and jobs, i.e. does not call addJob*()
	// or removeJob*() during the call to job.run(), the subclass may
	// set respectFailFast to true in order to speed up execution even
	// if slightly. The variable is initialized to false by default.
	protected boolean respectFailFast;

	protected final Lock mutex;
	private final Object pillow;
	private final Thread executingThread;
	private final HashMap<Long, Set<ScheduledJob>> jobs;
	private final HashMap<ScheduledJob, Long> reverse;
	private boolean running, terminated;
	private SortedSet<Long> timepoints;
	private long nextWakeUp;

	private static final long MINIMUM_SLEEP_PERIOD = 1000; // 1 second.
	private static final long SLEEP_TIL_INTERRUPTED = 0;

	public ScheduledJobExecutor(String threadName) {
		respectFailFast = false;
		mutex = new ReentrantLock(true); // fair.
		pillow = new Object();
		timepoints = new TreeSet<Long>(LongComparator.INSTANCE);
		jobs = new HashMap<Long, Set<ScheduledJob>>();
		reverse = new HashMap<ScheduledJob, Long>();
		running = true;
		terminated = false;
		nextWakeUp = Long.MAX_VALUE;
		executingThread = JThread.newDaemonThread(threadName, this);
		executingThread.setPriority(Thread.MAX_PRIORITY);
		executingThread.start();
	}

	public long wakesUpAt() {
		synchronized (pillow) {
			return nextWakeUp;
		}
	}

	public void addJob(ScheduledJob job) {
		mutex.lock();
		try {
			addJobNoLock(job);
		} finally {
			mutex.unlock();
		}
	}

	// This method is only protected but not private because subclasses
	// may choose to run it by locking the protected mutex themselves.
	protected void addJobNoLock(ScheduledJob job) {
		removeJobNoLock(job);
		timepoints.add(job.timepoint);
		Set<ScheduledJob> s = jobs.get(job.timepoint);
		if (s == null) {
			s = new HashSet<ScheduledJob>();
			jobs.put(job.timepoint, s);
		}
		s.add(job);
		reverse.put(job, job.timepoint);
		// Wake up the executing thread if necessary.
		synchronized (pillow) { 				 	 // Need to lock pillow to access nextWakeUp.
			if (job.timepoint < nextWakeUp) {		 // This job becomes runnable sooner than
				long d = nextWakeUp - job.timepoint; // the executing thread's next wake up;
				if (d >= MINIMUM_SLEEP_PERIOD)		 // interrupt the executing thread so that
					executingThread.interrupt();	 // it updates its internals.
			}
		}
	}

	public void removeJob(ScheduledJob job) {
		mutex.lock();
		try {
			removeJobNoLock(job);
		} finally {
			mutex.unlock();
		}
	}

	// This method is only protected but not private because subclasses
	// may choose to run it by locking the protected mutex themselves.
	protected void removeJobNoLock(ScheduledJob job) {
		Long timepoint = reverse.get(job);
		if (timepoint != null) {
			Set<ScheduledJob> s = jobs.get(timepoint);
			s.remove(job);
			if (s.isEmpty()) {
				timepoints.remove(timepoint);
				jobs.remove(timepoint);
			}
			reverse.remove(job);
		}
	}

	public void run() {
		Logger logger = Logger.getLogger(this.getClass());
		logger.log(Level.INFO, "Up and running");
		while (running) {
			long sleep = SLEEP_TIL_INTERRUPTED;
			logger.log(Level.DEBUG, "Woke up");
			mutex.lock();
			try {
				if (timepoints.size() > 0) { // Leave immediately when no responses are stored.
					long cutoff = System.currentTimeMillis();
					// If the subclass does not respect fail fast iteration, i.e. calls
					// addJob*() or removeJob*() in job.run(), new sets are constructed
					// to avoid ConcurrentModificationException.
					SortedSet<Long> expired = timepoints.headSet(cutoff);
					if (respectFailFast == false)
						expired = new TreeSet<Long>(expired);
					for (Long timepoint: expired) {
						Set<ScheduledJob> jobsToRun = jobs.get(timepoint);
						if (respectFailFast == false)
							jobsToRun = new HashSet<ScheduledJob>(jobsToRun);
						for (ScheduledJob job: jobsToRun) {
							job.run();
							reverse.remove(job);
						}
						jobs.remove(timepoint);
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
					if (running)
						pillow.wait(sleep);
				} catch (InterruptedException leave) {
				}
			}
		}
		synchronized (pillow) {
			pillow.notify();
			terminated = true;
		}
		logger.log(Level.INFO, "Terminated");
	}

	public void shutdown() {
		synchronized (pillow) {
			running = false;
			executingThread.interrupt();
			// Wait until the executing thread stops running.
			while (terminated == false) {
				try {
					pillow.wait();
				} catch (InterruptedException e) {
				}
			}
		}
		cleanUp();
	}

	protected abstract void cleanUp();
}
