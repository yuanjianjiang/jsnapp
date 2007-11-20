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

package org.jsnap.exception.server;

import java.io.IOException;

import org.apache.log4j.Level;

public final class ListenerStartException extends ListenerException {
	private static final long serialVersionUID = -2299976071834479167L;

	private static final String code = "01002";
	private static final String message = "Listener could not be started";

	public ListenerStartException(int port, IOException cause) {
		super(code, port, message, cause);
	}

	protected Level logLevel() {
		return Level.ERROR;
	}
}
