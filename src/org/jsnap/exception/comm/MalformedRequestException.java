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

package org.jsnap.exception.comm;

import org.apache.log4j.Level;
import org.jsnap.exception.JSnapException;

public final class MalformedRequestException extends JSnapException {
	private static final long serialVersionUID = -8578580564086424020L;

	private static final String code = "03001";
	private static final String message = "Malformed request: " /* %s */;

	public MalformedRequestException(String errmsg) {
		super(code, message + errmsg);
	}

	protected Level logLevel() {
		return Level.DEBUG;
	}
}
