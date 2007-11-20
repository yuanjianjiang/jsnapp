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

public class DbParam {
	public final int type;
	public final String other;
	public final String value;
	public final boolean isNull;

	public DbParam(int type, String other, String value, boolean isNull) {
		this.type = type;
		if (type == VENDOR_SPECIFIC)
			this.other = other;
		else
			this.other = "";
		this.value = value;
		this.isNull = (isNull || value == null);
	}

	public DbParam(int type, String value, boolean isNull) {
		this.type = type;
		this.other = "";
		this.value = value;
		this.isNull = (isNull || value == null);
	}

	public DbParam(String other, String value, boolean isNull) {
		this.type = VENDOR_SPECIFIC;
		this.other = other;
		this.value = value;
		this.isNull = (isNull || value == null);
	}

	public String toString() {
		return type + "-" + other + "-" + value + "-" + isNull;
	}

	public static final int VENDOR_SPECIFIC = 0;
	public static final int STRING = 1;
	public static final int BYTE = 2;
	public static final int SHORT = 3;
	public static final int INTEGER = 4;
	public static final int LONG = 5;
	public static final int FLOAT = 6;
	public static final int DOUBLE = 7;
	public static final int NUMERIC = 8;
	public static final int BOOLEAN = 9;
	public static final int DATE = 10;
	public static final int TIME = 11;
	public static final int TIMESTAMP = 12;
}
