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

package org.jsnap.server;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jsnap.exception.server.ListenerStartException;
import org.jsnap.server.Listener.ListenerInfo;
import org.jsnap.util.JThread;

public final class ListenerContainer {
	private HashMap<Integer, Listener> container;
	private final Lock modification;

	public ListenerContainer() {
		container = new HashMap<Integer, Listener>();
		modification = new ReentrantLock(true); // fair.
	}

	public void update(Listener[] listeners) {
		modification.lock(); // single thread accesses the container at a time.
		HashMap<Integer, Listener> containerNew = new HashMap<Integer, Listener>();
		try {
			for (Listener listener: listeners) {
				boolean register = false;
				Listener existing = container.get(listener.port);
				if (existing == null) {
					// New listener: start and register.
					register = true;
				} else {
					// A listener already listens to the same port. Check if equal.
					if (existing.equals(listener) == false) {
						// Two listeners are not equal: stop and unregister the existing one,
						// start and register the new one.
						existing.stop();
						Thread.yield(); // Solves the "address already in use" problem.
						register = true;
					} else {
						containerNew.put(existing.port, existing);
					}
					container.remove(listener.port);
				}
				if (register) {
					try {
						listener.start();
						containerNew.put(listener.port, listener);
						Thread listenerThread = JThread.newDaemonThread(listener.getClass().getSimpleName() + "@" + listener.port, listener);
						listenerThread.start();
					} catch (ListenerStartException e) {
						e.log(); // logged and skipped.
					}
				}
			}
			// At this point container HashMap contains the listeners that must be
			// stopped because they don't exist in the listeners array.
			for (Integer port: container.keySet()) {
				Listener listener = container.get(port);
				listener.stop();
			}
			// Final assignment, assign the new container to the actual container.
			container = containerNew;
		} finally {
			modification.unlock();
		}
	}

	public void shutdown() {
		modification.lock(); // single thread accesses the container at a time.
		try {
			for (Integer port: container.keySet()) {
				Listener listener = container.get(port);
				listener.stop();
			}
			container.clear();
		} finally {
			modification.unlock();
		}
	}

	public ListenerInfo[] info() {
		modification.lock(); // single thread accesses the container at a time.
		try {
			int i = 0;
			ListenerInfo[] list = new ListenerInfo[container.size()];
			for (Listener listener: container.values()) {
				list[i] = listener.info();
				++i;
			}
			return list;
		} finally {
			modification.unlock();
		}
	}
}
