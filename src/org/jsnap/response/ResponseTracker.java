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

package org.jsnap.response;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsnap.db.base.DbInstance;
import org.jsnap.exception.db.AccessKeyException;
import org.jsnap.exception.db.CommitException;
import org.jsnap.exception.db.CommittedException;
import org.jsnap.exception.db.InstanceInactiveException;
import org.jsnap.exception.db.RollbackException;
import org.jsnap.exception.db.RolledbackException;
import org.jsnap.exception.security.CredentialMismatchException;
import org.jsnap.security.Credentials;
import org.jsnap.util.JThread;

public final class ResponseTracker implements Runnable {
	private static final long STATUS_QUERY_PERIOD = 60000; // 1 minute.

	// Instances of ResponseIdentifier are compared by their keys (see equals),
	// ordered by their expiry times. (see ResponseIdentifierComparator.)
	public static class ResponseIdentifier {
		public final int key;
		public final long until;

		public boolean equals(Object obj) {
			if (obj instanceof ResponseIdentifier) {
				ResponseIdentifier rId = (ResponseIdentifier)obj;
				return (this.key == rId.key);
			}
			return false;
		}

		public int hashCode() {
			return key;
		}

		public String toString() {
			return Integer.toString(key);
		}

		// Instances of ResponseKey are created only by the ResponseTracker class.
		private ResponseIdentifier() {
			this(0, 0);
		}

		private ResponseIdentifier(int key) {
			this(key, 0);
		}

		private ResponseIdentifier(int key, long keepalive) {
			this.key = key;
			this.until = System.currentTimeMillis() + keepalive;
		}
	}

	private static class ResponseIdentifierComparator implements Comparator<ResponseIdentifier> {
		public static final ResponseIdentifierComparator INSTANCE = new ResponseIdentifierComparator();

		public boolean equals(Object obj) {
			return (obj instanceof ResponseIdentifierComparator);
		}

		public int compare(ResponseIdentifier o1, ResponseIdentifier o2) {
			if (o1.until < o2.until)
				return -1;
			else if (o1.until > o2.until)
				return 1;
			return 0;
		}
	}

	public class StoredResponse {
		private boolean f, cm, pn;
		private final DbInstance dbi;
		public final Response response;
		public final ResponseIdentifier identifier;

		// Instances of StoredResponse are created only by the ResponseTracker class.
		private StoredResponse(ResponseIdentifier identifier, Response response, DbInstance dbi) {
			this.dbi = dbi;
			this.response = response;
			this.response.setOwner(this);
			this.identifier = identifier;
			this.f = false;
			this.cm = false;
			this.pn = true;
		}

		public void check(Credentials credentials) throws CredentialMismatchException, AccessKeyException {
			if (response.credentials.equals(credentials) == false) {
				throw new CredentialMismatchException();
			} else if (f) {
				if (cm)
					throw new CommittedException();
				else
					throw new RolledbackException();
			}
		}

		public void commit() throws CommitException, InstanceInactiveException {
			dbi.commit();
			cm = true;
			pn = false;
			Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Committed (key: " + identifier.key + ")");
		}

		public void rollback() throws RollbackException, InstanceInactiveException {
			dbi.rollback();
			pn = false;
			Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Rolled back (key: " + identifier.key + ")");
		}

		public boolean pending() {
			return pn;
		}

		public boolean committed() {
			return cm;
		}

		public boolean closed() {
			return f;
		}

		public void close() {
			closeInternal();
			preserveForStatusQuery(this);
		}

		private void closeNoLock() {
			closeInternal();
			preserveForStatusQueryNoLock(this);
		}

		private void clearNoLock() {
			closeInternal();
			removeFromTrackerNoLock(this);
		}

		private void closeInternal() {
			if (f == false) {
				response.close();
				dbi.close();
			}
			f = true;
		}
	}

	private final Lock mutex;
	private final Random random;
	private final Thread executingThread;
	private SortedSet<ResponseIdentifier> keys;
	private final HashMap<ResponseIdentifier, StoredResponse> store;
	private boolean running = true, terminated = false;
	private Object pillow = new Object();
	private long nextWakeUp;

	private static final long MINIMUM_SLEEP_PERIOD = 1000; // 1 second.
	private static final long SLEEP_TIL_INTERRUPTED = 0;

	public ResponseTracker() {
		mutex = new ReentrantLock(true); // fair.
		random = new Random();
		keys = new TreeSet<ResponseIdentifier>(ResponseIdentifierComparator.INSTANCE);
		store = new HashMap<ResponseIdentifier, StoredResponse>();
		executingThread = JThread.newDaemonThread("RpsTracker", this);
		executingThread.setPriority(Thread.MAX_PRIORITY);
		executingThread.start();
		nextWakeUp = Long.MAX_VALUE;
	}

	// Wraps the three objects sent as parameters into a StoredResponse object.
	// Also generates and assigns a unique key for the newly created StoredResponse.
	// Returns the generated StoredResponse. Returned message is also stored internally
	// for future cleanup, if necessary. 
	public StoredResponse create(long keepalive, Response response, DbInstance dbi) {
		StoredResponse sr;
		mutex.lock();
		try {
			// Find a unique key.
			int key = -1;
			while (key <= 0) {
				key = random.nextInt();
				if (key < 0)
					key = -key; // Do not allow negative key values.
				if (keys.contains(new ResponseIdentifier(key)))
					key = -1; // Produce another key, this is not unique.
			}
			// Associate response with the unique key.
			response.setKey(key);
			// Create a response identifier with the unique key found above.
			ResponseIdentifier rId = new ResponseIdentifier(key, keepalive);
			// Create the stored response and store it internally.
			sr = new StoredResponse(rId, response, dbi);
			keys.add(rId);
			store.put(rId, sr);
			// Wake up the clean up thread if necessary.
			synchronized (pillow) { 				 // Need to lock pillow to access nextWakeUp.
				if (rId.until < nextWakeUp) { 		 // This response expires sooner than
					long d = nextWakeUp - rId.until; // the clean up thread's next wake up;
					if (d >= MINIMUM_SLEEP_PERIOD)	 // interrupt the clean up thread so that
						executingThread.interrupt(); // it updates its internals.
				}
			}
		} finally {
			mutex.unlock();
		}
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Created a stored response (key: " + sr.identifier + ")");
		return sr;
	}

	private void preserveForStatusQuery(StoredResponse sr) {
		mutex.lock();
		try {
			preserveForStatusQueryNoLock(sr);
			synchronized (pillow) {
				pillow.notify();
			}
		} finally {
			mutex.unlock();
		}
	}

	private void preserveForStatusQueryNoLock(StoredResponse sr) {
		keys.remove(sr.identifier);
		keys.add(new ResponseIdentifier(sr.identifier.key, STATUS_QUERY_PERIOD));
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Stored response is in status inquiry state (key: " + sr.identifier + ")");
	}

	private void removeFromTrackerNoLock(StoredResponse sr) {
		keys.remove(sr.identifier);
		store.remove(sr.identifier);
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Cleaned up a stored response (key: " + sr.identifier + ")");
	}

	// Returns the StoredResponse associated with the given key.
	public StoredResponse get(int key) {
		mutex.lock();
		try {
			return store.get(new ResponseIdentifier(key));
		} finally {
			mutex.unlock();
		}
	}

	public void run() {
		Logger logger = Logger.getLogger(ResponseTracker.class);
		logger.log(Level.INFO, "Up and running");
		while (running) {
			long sleep = SLEEP_TIL_INTERRUPTED;
			logger.log(Level.DEBUG, "Woke up");
			mutex.lock();
			try {
				if (keys.size() > 0) { // Leave immediately when no responses are stored.
					ResponseIdentifier cutoff = new ResponseIdentifier();
					SortedSet<ResponseIdentifier> expired = new TreeSet<ResponseIdentifier>(keys.headSet(cutoff));
					for (ResponseIdentifier rId: expired) {
						StoredResponse response = store.get(rId);
						synchronized (response) {
							if (response.closed())
								response.clearNoLock();
							else
								response.closeNoLock();
						}
					}
					keys = keys.tailSet(cutoff);
					if (keys.size() > 0) {
						sleep = keys.first().until - cutoff.until;
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

	private void cleanUp() {
		mutex.lock();
		try {
			// Allocate a new set to avoid ConcurrentModificationException.
			Set<ResponseIdentifier> storedKeys = new HashSet<ResponseIdentifier>(store.keySet());
			for (ResponseIdentifier rId: storedKeys) {
				StoredResponse response = store.get(rId);
				synchronized (response) {
					response.clearNoLock();
				}
			}
			keys.clear();
		} finally {
			mutex.unlock();
		}
		Logger.getLogger(ResponseTracker.class).log(Level.INFO, "Stored responses are cleaned up");
	}
}
