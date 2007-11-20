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

package org.jsnap.request;

import java.net.Socket;
import java.util.ArrayList;

import org.jsnap.db.base.DbParam;
import org.jsnap.security.Credentials;
import org.jsnap.util.JUtility;

public abstract class DelimitedPacking extends DbRequest {
	private static final byte SEPARATOR_BYTE = ',';
	private static final char SEPARATOR_CHAR = ',';
	private static final byte DELIMITER_BYTE = ';';
	private static final char DELIMITER_CHAR = ';';

	public DelimitedPacking() {	// Default constructor required
		super();				// by the Listener class.
	}

	public DelimitedPacking(long acceptedOn, Socket s) {
		super(acceptedOn, s);
	}

	protected byte[] doPack() {
		String credentialsString = "";
		String[] credentialsArray = credentials.get();
		for (int i = 0; i < credentialsArray.length; ++i) {
			if (i > 0)
				credentialsString += SEPARATOR_CHAR;
			credentialsString += escape(credentialsArray[i], SEPARATOR_BYTE);
		}
		String parametersString = "";
		for (int i = 0; i < parameters.size(); ++i) {
			if (i > 0)
				parametersString += SEPARATOR_CHAR;
			DbParam param = parameters.get(i);
			parametersString += (Integer.toString(param.type) + SEPARATOR_CHAR + escape(param.other, SEPARATOR_BYTE) + SEPARATOR_CHAR + escape(param.value, SEPARATOR_BYTE) + SEPARATOR_CHAR + Boolean.toString(param.isNull));
		}
		String packed = escape(command, DELIMITER_BYTE) + DELIMITER_CHAR;
		packed += escape(database, DELIMITER_BYTE) + DELIMITER_CHAR;
		packed += escape(sql, DELIMITER_BYTE) + DELIMITER_CHAR;
		packed += escape(formatter, DELIMITER_BYTE) + DELIMITER_CHAR;
		packed += Long.toString(timeout) + DELIMITER_CHAR;
		packed += Long.toString(keepalive) + DELIMITER_CHAR;
		packed += Integer.toString(zip) + DELIMITER_CHAR;
		packed += Integer.toString(key) + DELIMITER_CHAR;
		packed += Integer.toString(from) + DELIMITER_CHAR;
		packed += Integer.toString(to) + DELIMITER_CHAR;
		packed += Integer.toString(maxrows) + DELIMITER_CHAR;
		packed += escape(credentialsString, DELIMITER_BYTE) + DELIMITER_CHAR;
		packed += escape(parametersString, DELIMITER_BYTE) + DELIMITER_CHAR;
		return packed.getBytes();
	}

	protected void doUnpack(byte[] packed, int offset, int length) {
		String[] splitted = split(packed, offset, length, DELIMITER_BYTE);
		command = JUtility.valueOf((splitted.length > 0 ? splitted[0] : null), "").trim();
		database = JUtility.valueOf((splitted.length > 1 ? splitted[1] : null), "").trim();
		sql = JUtility.valueOf((splitted.length > 2 ? splitted[2] : null), "").trim();
		formatter = JUtility.valueOf((splitted.length > 3 ? splitted[3] : null), "").trim();
		timeout = JUtility.valueOf((splitted.length > 4 ? splitted[4] : null), -1L); 	// long
		keepalive = JUtility.valueOf((splitted.length > 5 ? splitted[5] : null), -1L);	// long
		zip = JUtility.valueOf((splitted.length > 6 ? splitted[6] : null), -1); 		// int
		key = JUtility.valueOf((splitted.length > 7 ? splitted[7] : null), -1); 		// int
		from = JUtility.valueOf((splitted.length > 8 ? splitted[8] : null), -1); 		// int
		to = JUtility.valueOf((splitted.length > 9 ? splitted[9] : null), -1); 			// int
		maxrows = JUtility.valueOf((splitted.length > 10 ? splitted[10] : null), -1); 	// int
		if (splitted.length > 11 && splitted[11] != null)
			credentials = new Credentials(split(splitted[11].getBytes(), 0, splitted[11].length(), SEPARATOR_BYTE));
		String[] params = (splitted.length > 12  && splitted[12] != null ? split(splitted[12].getBytes(), 0, splitted[12].length(), SEPARATOR_BYTE) : new String[0]);
		int paramCount = params.length / 4;
		for (int p = 0; p < paramCount; ++p) {
			int type = JUtility.valueOf(params[4 * p], DbParam.STRING);
			String other = params[4 * p + 1];
			String pvalue = params[4 * p + 2];
			boolean isNull = Boolean.valueOf(params[4 * p + 3]);
			parameters.add(new DbParam(type, other, pvalue, isNull));						
		}
	}

	private String[] split(byte[] delimited, int offset, int length, byte delimiter) {
		Integer[] indices = findDelimiters(delimited, offset, length, delimiter);
		String[] splitted = new String[indices.length];
		int start = offset;
		for (int i = 0; i < indices.length; ++i) {
			splitted[i] = clean(new String(delimited, start, indices[i] - start), DELIMITER_BYTE);
			start = indices[i] + 1;
		}
		return splitted;
	}

	private Integer[] findDelimiters(byte[] delimited, int offset, int length, byte delimiter) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		int i, j;
		for (i = 0, j = offset; i < length && j < delimited.length; ++i, ++j) {
			if (delimited[j] == delimiter) {
				if ((i + 1) == length || (j + 1) == delimited.length) {
					indices.add(j);
				} else {
					if (delimited[j + 1] == delimiter) {
						++i;
						++j;
					} else {
						indices.add(j);
					}
				}
			}
		}
		if (delimited[j - 1] != delimiter)	// Even if the last character was not a
			indices.add(j);					// delimiter, assume that there was one.
		Integer[] indicesArray = new Integer[indices.size()];
		indices.toArray(indicesArray);
		return indicesArray;
	}

	// This function finds the delimiter characters in the original string
	// and escapes each delimiter character by doubling them.
	private String escape(String original, byte escapeChar) {
		if (original == null || original.equals(""))
			return "(null)";
		byte[] orig = original.getBytes();
		int c = 0;
		for (byte o: orig) {
			if (o == escapeChar)
				++c;
		}
		int i = 0;
		byte[] escaped = new byte[orig.length + c];
		for (byte o: orig) {
			if (o == escapeChar) {
				escaped[i] = escapeChar;
				++i;
				escaped[i] = escapeChar;
				++i;
			} else {
				escaped[i] = o;
				++i;
			}
		}
		return new String(escaped);
	}

	// Given a string in which the delimiter characters are escaped, this
	// function removes the escape characters in front of the delimiters.
	private String clean(String escaped, byte escapedChar) {
		if (escaped == null || escaped.equals("(null)"))
			return null;
		byte[] esc = escaped.getBytes();
		int c = 0;
		for (int i = 0; i < esc.length; ++i) {
			if (esc[i] == escapedChar && (i + 1) < esc.length && esc[i + 1] == escapedChar)
				++c;
		}
		int p = 0;
		byte[] orig = new byte[esc.length - c];
		for (int i = 0; i < esc.length; ++i) {
			if (esc[i] == escapedChar && (i + 1) < esc.length && esc[i + 1] == escapedChar) {
				orig[p] = escapedChar;
				++p;
				++i;
			} else {
				orig[p] = esc[i];
				++p;
			}
		}
		return new String(orig);
	}
}
