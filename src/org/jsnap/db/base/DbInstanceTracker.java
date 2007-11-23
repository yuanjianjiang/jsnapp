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

import org.jsnap.util.ScheduledJobExecutor;

public final class DbInstanceTracker extends ScheduledJobExecutor {
	private static class DisconnectJob extends ScheduledJob<DbInstance> {
		private boolean log;

		public DisconnectJob(DbInstance dbi) {
			this(IGNORED_TIMEPOINT, dbi);
		}

		public DisconnectJob(long timepoint, DbInstance dbi) {
			super(timepoint, dbi);
			log = true;
		}

		public void setLogging(boolean log) {
			this.log = log;
		}

		public void run() {
			DbInstance dbi = (DbInstance)object;
			synchronized (dbi) {
				if (timepoint == IGNORED_TIMEPOINT) {
					dbi.disconnect(log);
				} else {
					long lastActive = dbi.getLastActive();
					if (lastActive < timepoint)
						dbi.disconnect(log);
				}
			}
		}
	}

	public DbInstanceTracker() {
		super("DbiTracker");
		respectFailFast = true; // DisconnectJob.run() does not call addJob*() or
	}							// removeJob*(). See declaration of respectFailFast. 

	public void disconnectIfIdleAt(long timepoint, DbInstance dbi, boolean log) {
		if (dbi.connected())
			addJob(new DisconnectJob(timepoint, dbi)); 
	}

	public void disconnectImmediately(DbInstance dbi, boolean log) {
		if (dbi.connected()) {
			DisconnectJob job = new DisconnectJob(0, dbi);
			job.setLogging(log);
			removeJob(job);
			job.run();
		}
	}

	protected void cleanUp() {
		// No operation; all active instances will first get closed by ResponseTracker
		// and then all instances will get disconnected by Dbregistry.
	}
}
