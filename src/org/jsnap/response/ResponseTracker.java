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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
import org.jsnap.util.ScheduledJobExecutor;

public final class ResponseTracker extends ScheduledJobExecutor {
	private static final long STATUS_QUERY_PERIOD = 60000; // 1 minute.

	private class ResponseTrackingJob extends ScheduledJob<StoredResponse> {
		public ResponseTrackingJob(StoredResponse response) {
			super(IGNORED_TIMEPOINT, response);
		}

		public ResponseTrackingJob(long timepoint, StoredResponse response) {
			super(timepoint, response);
		}

		public int hashCode() {
			return object.hashCode();
		}

		public boolean equals(Object obj) {
			if (obj != null && obj instanceof ResponseTrackingJob) {
				ResponseTrackingJob job = (ResponseTrackingJob)obj;
				return (object.equals(job.object));
			}
			return false;
		}

		public void run() {
			StoredResponse sr = object;
			synchronized (sr) {
				if (sr.closed())
					sr.clearNoLock();
				else
					sr.closeNoLock();
			}
		}
	}

	public class StoredResponse {
		private boolean f, cm, pn;
		private final DbInstance dbi;
		public final Response response;
		public final int key;

		// Instances of StoredResponse are created only by the ResponseTracker class.
		private StoredResponse(int key, Response response, DbInstance dbi) {
			this.dbi = dbi;
			this.response = response;
			this.response.setOwner(this);
			this.key = key;
			this.f = false;
			this.cm = false;
			this.pn = true;
		}

		public int hashCode() {
			return key;
		}

		public boolean equals(Object obj) {
			if (obj != null && obj instanceof StoredResponse) {
				StoredResponse sr = (StoredResponse)obj;
				return (key == sr.key);
			}
			return false;
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
			Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Committed (key: " + key + ")");
		}

		public void rollback() throws RollbackException, InstanceInactiveException {
			dbi.rollback();
			pn = false;
			Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Rolled back (key: " + key + ")");
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

	// super.mutex is used while accessing these private variables:
	private final Random random;
	private final Set<Integer> keys;
	private final HashMap<Integer, StoredResponse> store;

	public ResponseTracker() {
		super ("RpsTracker");
		random = new Random();
		keys = new HashSet<Integer>();
		store = new HashMap<Integer, StoredResponse>();
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
				if (keys.contains(key))
					key = -1; // Produce another key, this is not unique.
			}
			// Associate response with the unique key.
			response.setKey(key);
			// Create the stored response and store it internally.
			long expiresAt = System.currentTimeMillis() + keepalive;
			sr = new StoredResponse(key, response, dbi);
			addJobNoLock(new ResponseTrackingJob(expiresAt, sr));
			keys.add(key);
			store.put(key, sr);
		} finally {
			mutex.unlock();
		}
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Created a stored response (key: " + sr.key + ")");
		return sr;
	}

	private void preserveForStatusQuery(StoredResponse sr) {
		mutex.lock();
		try {
			preserveForStatusQueryNoLock(sr);
		} finally {
			mutex.unlock();
		}
	}

	private void preserveForStatusQueryNoLock(StoredResponse sr) {
		long now = System.currentTimeMillis();
		addJobNoLock(new ResponseTrackingJob(now + STATUS_QUERY_PERIOD, sr)); // Overwrites the old one.
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Stored response is in status inquiry state (key: " + sr.key + ")");
	}

	private void removeFromTrackerNoLock(StoredResponse sr) {
		removeJobNoLock(new ResponseTrackingJob(sr));
		keys.remove(sr.key);
		store.remove(sr.key);
		Logger.getLogger(ResponseTracker.class).log(Level.DEBUG, "Cleaned up a stored response (key: " + sr.key + ")");
	}

	// Returns the StoredResponse associated with the given key.
	public StoredResponse get(int key) {
		mutex.lock();
		try {
			return store.get(key);
		} finally {
			mutex.unlock();
		}
	}

	protected void cleanUp() {
		mutex.lock();
		try {
			Set<Integer> copy = new HashSet<Integer>(keys); // To avoid ConcurrentModificationException.
			for (int key: copy) {
				StoredResponse response = store.get(key);
				synchronized (response) {
					response.clearNoLock(); // Modifies keys.
				}
			}
		} finally {
			mutex.unlock();
		}
		Logger.getLogger(ResponseTracker.class).log(Level.INFO, "Stored responses are cleaned up");
	}
}
