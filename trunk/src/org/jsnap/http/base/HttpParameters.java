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

package org.jsnap.http.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class HttpParameters {
	private final HashMap<String, Set<String>> parameters;

	public HttpParameters() {
		parameters = new HashMap<String, Set<String>>();
	}

	public void put(String name, String value) {
		Set<String> set = parameters.get(name);
		if (set == null) {
			set = new HashSet<String>();
			parameters.put(name, set);
		}
		set.add(value);
	}

	public String get(String name) {
		String value = null;
		Set<String> set = parameters.get(name);
		if (set != null)
			value = set.iterator().next();
		return value;
	}

	public Set<String> getAll(String name) {
		return parameters.get(name);
	}
}
