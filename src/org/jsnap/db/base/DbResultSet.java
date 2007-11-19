package org.jsnap.db.base;

import org.jsnap.exception.db.ResultSetException;

public interface DbResultSet {
	public String ownerdb();

	public boolean hasCursor();
	public int affectedRows();

	public void limitBetween(int start, int stop) throws ResultSetException;
	public void positionAt(int position) throws ResultSetException;
	public boolean next() throws ResultSetException;
	public int positionedAt();

	public Class typeMapping(int sqlType);
	public int getColumnCount();
	public DbHeader whatis(int index);
	public Object get(int index) throws ResultSetException;
	public Object get(String fieldName) throws ResultSetException;

	public void close(); 	// Calling close on an already closed DbResultSet should be a no-op. 
}
