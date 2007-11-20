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

import java.util.ArrayList;

import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.exception.db.SqlException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.util.JDocument;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;

public final class GroupManagement extends AbstractWebMultiPage {
	public static final GroupManagement INSTANCE = new GroupManagement();

	public String key() {
		return "groups";
	}

	public String name() {
		return "Group Management";
	}

	public String category() {
		return CATEGORY_MANAGEMENT;
	}

	public String stylesheet() {
		return "/jwc/groups.xsl";
	}

	public boolean administrative() {
		return true;
	}

	private static final String DOCUMENT_ROOT = "groups";
	private static final String RECORD_ROOT = "group";
	private static final String MESSAGE = "message";
	private static final String PARAMETER_GROUPNAME = "groupname";
	private static final String PARAMETER_ADMINISTRATIVE = "administrative";
	private static final String PARAMETER_GROUPID = "groupid";
	private static final String PARAMETER_NEWNAME = "newname";
	private static final String PARAMETER_REFRESH = "refresh";
	private static final String PARAMETER_DELETE = "delete";
	private static final String PARAMETER_INVERT = "invert";
	private static final String PARAMETER_RENAME = "rename";
	private static final String PARAMETER_CREATE = "create";

	private String groupname, newname;

	public GroupManagement() {
		super(DOCUMENT_ROOT, RECORD_ROOT);
	}

	protected WebResponse runOnce(HttpRequest request) {
		return null; // No operation.
	}

	protected void initialize() {
		// No operation.
	}

	protected void appendPageData(JDocument doc) {
		doc.appendTextNode(PARAMETER_GROUPNAME, groupname);
		doc.appendTextNode(PARAMETER_NEWNAME, newname);
	}

	protected boolean needsRefresh(HttpRequest request) {
		return (request.parameters.get(PARAMETER_REFRESH) != null);
	}

	protected void refresh(HttpRequest request) {
		groupname = JUtility.valueOf(request.parameters.get(PARAMETER_GROUPNAME), "").trim();
	}

	protected boolean operation(JDocument doc, HttpRequest request) {
		boolean refresh = false;
		int groupid = JUtility.valueOf(request.parameters.get(PARAMETER_GROUPID), 0);
		boolean delete = (request.parameters.get(PARAMETER_DELETE) != null);
		boolean invert = (request.parameters.get(PARAMETER_INVERT) != null);
		boolean rename = (request.parameters.get(PARAMETER_RENAME) != null);
		boolean create = (request.parameters.get(PARAMETER_CREATE) != null);
		int count = (delete ? 1 : 0) + (invert ? 1 : 0) + (rename ? 1 : 0) + (create ? 1 : 0);
		if (count > 1)
			delete = invert = rename = create = false; // Only one operation is permitted at a time.
		if (delete || invert || rename) {
			groupname = JUtility.valueOf(getGroupName(groupid), "");
			// If groupname is null it means that the groupid is not valid. That's a no operation.
			if (groupname.length() == 0)
				delete = invert = rename = false;
		}
		newname = (rename || create ? JUtility.valueOf(request.parameters.get(PARAMETER_NEWNAME), "").trim() : "");
		if (delete) {
			// An existing group is to be deleted.
			try {
				refresh = deleteGroup(doc, groupid);
			} catch (SqlException e) {
				e.log(); // logged and skipped.
				doc.appendTextNodeWithAttr(ERROR, e.getMessage(), new JPair<String, String>(AT, PARAMETER_DELETE));
			}
		} else if (invert) {
			// Administrative privileges of a group is to be inverted.
			try {
				refresh = invertPrivileges(doc, groupid);
			} catch (SqlException e) {
				e.log(); // logged and skipped.
				doc.appendTextNodeWithAttr(ERROR, e.getMessage(), new JPair<String, String>(AT, PARAMETER_INVERT));
			}
		} else if (rename) {
			// A group is to be renamed.
			if (newname.length() == 0) {
				doc.appendTextNodeWithAttr(ERROR, "Group name cannot be left blank.", new JPair<String, String>(AT, PARAMETER_RENAME));
			} else if (allowCreate(newname) == false) {
				doc.appendTextNodeWithAttr(ERROR, "A group named " + newname + " already exists.", new JPair<String, String>(AT, PARAMETER_RENAME));
			} else {
				try {
					renameGroup(groupid, newname);
					doc.appendTextNode(MESSAGE, "Group " + groupname + " renamed to " + newname + ".");
					groupname = newname;
					newname = "";
					refresh = true;
				} catch (SqlException e) {
					e.log(); // logged and skipped.
					doc.appendTextNodeWithAttr(ERROR, e.getMessage(), new JPair<String, String>(AT, PARAMETER_RENAME));
				}
			}
		} else if (create) {
			// A new group is to be created.
			if (newname.length() == 0) {
				doc.appendTextNodeWithAttr(ERROR, "A group name must be provided.", new JPair<String, String>(AT, PARAMETER_CREATE));
			} else if (allowCreate(newname) == false) {
				doc.appendTextNodeWithAttr(ERROR, "A group with the same name already exists.", new JPair<String, String>(AT, PARAMETER_CREATE));
				doc.appendTextNode(PARAMETER_NEWNAME, newname);
			} else {
				boolean administrative = (request.parameters.get(PARAMETER_ADMINISTRATIVE) != null);
				try {
					createGroup(newname, administrative);
					doc.appendTextNode(MESSAGE, "Created group " + newname + ".");
					groupname = newname;
					newname = "";
					refresh = true;
				} catch (SqlException e) {
					e.log(); // logged and skipped.
					doc.appendTextNodeWithAttr(ERROR, e.getMessage(), new JPair<String, String>(AT, PARAMETER_CREATE));
				}
			}
		}
		return refresh;
	}

	protected DbResultSet create(DbInstance dbi) throws SqlException {
		// Prepare SQL statement and parameters.
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		String sql = "SELECT g.groupid AS \"id\", g.groupname AS \"name\", " +
							"(CASE g.administrative WHEN true THEN 'yes' ELSE 'no' END) AS \"administrative\", " +
							"(SELECT COUNT(*) FROM memberships m WHERE m.groupid = g.groupid) AS \"members\", " +
							"1 AS \"counter\" " + // Required for AbstractWebMultiPage to work correctly.
		               "FROM groups g";
		if (groupname.length() > 0) {
			sql += " WHERE g.groupname LIKE ?";
			parameters.add(new DbParam(DbParam.STRING, groupname.replace('*', '%'), false));
		}
		sql += " ORDER BY 3 DESC, 2";
		// Run the SQL.
		DbStatement statement = dbi.createStatement(sql, parameters, true);
		DbResultSet result = statement.execute();
		return result;
	}

	private String getGroupName(int groupid) {
		String name = null;
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(groupid), false));
				DbStatement statement = dbi.createStatement("SELECT groupname FROM groups WHERE groupid = ?", parameters, false);
				DbResultSet result = statement.execute();
				try {
					while (result.next())
						name = (String)result.get("groupname");
				} finally {
					result.close();
				}
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
		}
		return name;
	}

	private boolean allowCreate(String groupname) {
		boolean unique = true;
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				parameters.add(new DbParam(DbParam.STRING, groupname, false));
				DbStatement statement = dbi.createStatement("SELECT groupid FROM groups WHERE groupname = ?", parameters, false);
				DbResultSet result = statement.execute();
				try {
					while (unique && result.next())
						unique = false;
				} finally {
					result.close();
				}
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
		}
		return unique;
	}

	private void createGroup(String groupname, boolean administrative) throws SqlException {
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		parameters.add(new DbParam(DbParam.STRING, groupname, false));
		parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(administrative), false));
		tableUpdate("INSERT INTO groups(groupname, administrative) VALUES(?, ?)", parameters);
	}


	private void renameGroup(int groupid, String newname) throws SqlException {
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		parameters.add(new DbParam(DbParam.STRING, newname, false));
		parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(groupid), false));
		tableUpdate("UPDATE groups SET groupname = ? WHERE groupid = ?", parameters);
	}

	private void tableUpdate(String sql, ArrayList<DbParam> parameters) throws SqlException {
		DbInstance dbi = getDbInstance();
		try {
			DbStatement statement = dbi.createStatement(sql, parameters, false);
			statement.execute().close();
			dbi.commit();
		} finally {
			dbi.close();
		}
	}

	private boolean deleteGroup(JDocument doc, int groupid) throws SqlException {
		DbInstance dbi = getDbInstance();
		try {
			ArrayList<DbParam> parameters = new ArrayList<DbParam>();
			parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(groupid), false));
			dbi.createStatement("DELETE FROM memberships WHERE groupid = ?", parameters, false).execute();
			dbi.createStatement("DELETE FROM groups WHERE groupid = ?", parameters, false).execute();
			boolean activeAdminExists = UserManagement.activeAdminExists(dbi);
			if (activeAdminExists) {
				dbi.commit();
				doc.appendTextNode(MESSAGE, "Group " + groupname + " deleted.");
				return true;
			} else {
				dbi.rollback();
				doc.appendTextNodeWithAttr(ERROR, "Deleting the group " + groupname + " would remove all active administrators from the system and therefore is not allowed.", new JPair<String, String>(AT, PARAMETER_DELETE));
				return false;
			}
		} finally {
			dbi.close();
		}
	}

	private boolean invertPrivileges(JDocument doc, int groupid) throws SqlException {
		DbInstance dbi = getDbInstance();
		try {
			ArrayList<DbParam> parameters = new ArrayList<DbParam>();
			parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(groupid), false));
			dbi.createStatement("UPDATE groups SET administrative = (NOT administrative) WHERE groupid = ?", parameters, false).execute();
			boolean activeAdminExists = UserManagement.activeAdminExists(dbi);
			if (activeAdminExists) {
				dbi.commit();
				doc.appendTextNode(MESSAGE, "Administrative privileges altered.");
				return true;
			} else {
				dbi.rollback();
				doc.appendTextNodeWithAttr(ERROR, "Downgrading the group's privileges would remove all active administrators from the system and therefore is not allowed.", new JPair<String, String>(AT, PARAMETER_INVERT));
				return false;
			}
		} finally {
			dbi.close();
		}
	}
}
