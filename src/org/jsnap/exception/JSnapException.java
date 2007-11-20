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

package org.jsnap.exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class JSnapException extends Exception {
	public static JSnapException create(byte[] bytes) throws IOException {
		return create(bytes, 0, bytes.length);
	}

	public static JSnapException create(byte[] bytes, int offset) throws IOException {
		return create(bytes, offset, bytes.length - offset);
	}

	public static JSnapException create(byte[] bytes, int offset, int length) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes, offset, length);
		ObjectInputStream ois = new ObjectInputStream(bais);
		JSnapException ex = null;
		try {
			ex = (JSnapException)ois.readObject();
		} catch (ClassNotFoundException e) {
		}
		ois.close();
		return ex;
	}

	public JSnapException(String code, String message) {
		super("JSNAP-" + code + ": " + message);
	}

	public JSnapException(String code, String at, String message) {
		super("JSNAP-" + code + "(@" + at + "): " + message);
	}

	protected JSnapException(String code, String message, Throwable cause) {
		super("JSNAP-" + code + ": " + message, cause);
	}

	protected JSnapException(String code, String at, String message, Throwable cause) {
		super("JSNAP-" + code + "(@" + at + "): " + message, cause);
	}

	public void log() {
		Level level = logLevel();
		String message = getMessage();
		Logger.getLogger(this.getClass()).log(level, message, this);
	}

	public void asBytes(OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(this);
	}

	protected abstract Level logLevel();
}
