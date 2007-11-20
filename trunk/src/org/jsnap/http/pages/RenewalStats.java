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

package org.jsnap.http.pages;

import org.jsnap.db.base.Dbregistry;

public final class RenewalStats extends AuthenticationStats {
	public static final RenewalStats INSTANCE = new RenewalStats();

	public String logKey() {
		return "renew";
	}

	public String key() {
		return "stats-renew";
	}

	public String name() {
		return "Password Renewal Statistics";
	}

	protected RenewalStats() {
		super(Dbregistry.INTERNALDB_NAME);
	}
}
