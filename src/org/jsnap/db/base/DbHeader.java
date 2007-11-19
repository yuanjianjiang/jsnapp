package org.jsnap.db.base;

public class DbHeader {
	public final Class type;
	public final String name;
	public final int precision, scale;

	public DbHeader(Class type, String name, int precision, int scale) {
		this.type = type;
		this.name = name;
		this.precision = precision;
		this.scale = scale;
	}
}
