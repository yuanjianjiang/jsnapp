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

public final class WebAuthenticationStats extends AuthenticationStats {
	public static final WebAuthenticationStats INSTANCE = new WebAuthenticationStats();

	public String logKey() {
		return "auth-web";
	}

	public String key() {
		return "stats-auth-web";
	}

	public String name() {
		return "Authentication Statistics (Web Console)";
	}

	private WebAuthenticationStats() {
		super(Dbregistry.INTERNALDB_NAME);
	}
}
