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

import org.jsnap.exception.JSnapException;

public abstract class ListenerException extends JSnapException {
	protected ListenerException(String code, int port, String message, Throwable cause) {
		super(code, Integer.toString(port), message, cause);
	}

	protected ListenerException(String code, String message, Throwable cause) {
		super(code, message, cause);
	}

	protected ListenerException(String code, String message) {
		super(code, message);
	}
}
