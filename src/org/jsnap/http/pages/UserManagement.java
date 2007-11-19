package org.jsnap.http.pages;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.exception.db.SqlException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.response.Formatter;
import org.jsnap.security.Authenticate;
import org.jsnap.security.AuthenticationPolicy;
import org.jsnap.util.JDocument;
import org.jsnap.util.JPair;
import org.jsnap.util.JUtility;
import org.xml.sax.SAXException;

public final class UserManagement extends AbstractWebMultiPage {
	// TODO: Add operations: rename and password reset.

	public static final UserManagement INSTANCE = new UserManagement();

	public String key() {
		return "users";
	}

	public String name() {
		return "User Management";
	}

	public String category() {
		return CATEGORY_MANAGEMENT;
	}

	public String stylesheet() {
		return "/jwc/users.xsl";
	}

	public boolean administrative() {
		return true;
	}

	// Main XML tags:
	private static final String DOCUMENT_ROOT = "users";
	private static final String RECORD_ROOT = "user";
	// Criteria related XML tags:
	private static final String DIGEST_ALGO_ROOT = "mdalg";
	private static final String[] DIGEST_ALGO_NODES = new String[]{"algo", "different", "error", "show"};
	private static final String USER_CREATE_ROOT = "create";
	private static final String[] USER_CREATE_NODES = new String[]{"message", "error", "username", "password", "admin", "pwdexpired", "lockedout"};
	private static final String OPERATION_ROOT = "operation";
	private static final String[] OPERATION_NODES = new String[]{"message", "error"};
	private static final String GROUPS_ROOT = "groups";
	private static final String GROUPS_ITEM = "group";
	private static final String GROUP_ID = "id";
	private static final String GROUP_NAME = "name";
	private static final String DATABASES_ROOT = "databases";
	private static final String DATABASES_ITEM = "database";
	private static final String ACCOUNT_STATUS_ROOT = "status";
	private static final String[] ACCOUNT_STATUS_NODES = new String[]{"active", "expired", "locked"};
	private static final String ADMINISTRATIVE_RIGHTS_ROOT = "rights";
	private static final String[] ADMINISTRATIVE_RIGHTS_NODES = new String[]{"user", "group", "ordinary"};
	// HTTP parameter names:
	private static final String PARAMETER_REFRESH = "refresh";
	private static final String PARAMETER_CURRENT = "current";
	private static final String PARAMETER_OPERATION = "operation";
	private static final String PARAMETER_ALGORITHM = "algo";
	private static final String PARAMETER_CREATE_NAME = "newname";
	private static final String PARAMETER_CREATE_PASSWORD = "password";
	private static final String PARAMETER_CREATE_ADMIN = "admin";
	private static final String PARAMETER_CREATE_EXPIRED = "pwdexpired";
	private static final String PARAMETER_CREATE_LOCKED = "lockedout";
	private static final String PARAMETER_USERNAME = "username";
	private static final String PARAMETER_GROUP = "group";
	private static final String PARAMETER_DATABASE = "db";
	private static final String PARAMETER_ACCT_STATUS = "account";
	private static final String PARAMETER_ACCT_STATUS_ACTIVE = "active";
	private static final String PARAMETER_ACCT_STATUS_EXPIRED = "expired";
	private static final String PARAMETER_ACCT_STATUS_LOCKED = "locked";
	private static final String PARAMETER_ADMIN_RIGHTS = "rights";
	private static final String PARAMETER_ADMIN_RIGHTS_USER = "adminuser";
	private static final String PARAMETER_ADMIN_RIGHTS_GROUP = "admingroup";
	private static final String PARAMETER_ADMIN_RIGHTS_NONE = "ordinaryuser";
	private static final String PARAMETER_IPADDRESS = "ipaddr";
	private static final String PARAMETER_SELECTION = "selection";
	private static final String PARAMETER_SELECTION_ALL = "all";
	private static final String PARAMETER_SELECTION_SELECTED = "selected";
	private static final String PARAMETER_SELECTION_LISTEDUSER = "listeduser";
	private static final String PARAMETER_MEMBEROF = "memberof";
	private static final String STATE = "state";
	private static final String ON = "on";
	// Operation types that are valid for PARAMETER_OPERATION:
	private static final String OPERATION_LIST_ALL = "none";
	private static final String OPERATION_ALGO_MODIFY = "mdalgmodify";
	private static final String OPERATION_USER_CREATE = "create";
	private static final String OPERATION_USER_DELETE = "delete";
	private static final String OPERATION_ACCOUNT_LOCKOUT = "lockout";
	private static final String OPERATION_ACCOUNT_UNLOCK = "unlock";
	private static final String OPERATION_GRANT_PRIVILEGES = "grant";
	private static final String OPERATION_REVOKE_PRIVILEGES = "revoke";
	private static final String OPERATION_EXPIRE_PASSWORDS = "expire";
	//private static final String OPERATION_RESET_PASSWORDS = "reset";
	//private static final String OPERATION_USER_RENAME = "rename";
	private static final String OPERATION_VIEW_GROUPS = "viewgroups";
	private static final String OPERATION_UPDATE_GROUPS = "updategroups";
	private static final String OPERATION_QUERY_DIFFALGO = "qyalgodiff";

	private DbResultSet result;
	// Message digest algorithm related:
	private String mdalgAlgo, mdalgError;
	private int mdalgDifferent;
	private boolean mdalgShow;
	// Query related:
	private boolean qyalgodiff;
	private String username, ipaddr;
	private final Set<Integer> selectedGroups;
	private final Set<String> selectedDatabases;
	private boolean active, expired, locked, adminAsUser, adminFromGroup, ordinaryUser;

	public UserManagement() {
		super(DOCUMENT_ROOT, RECORD_ROOT);
		username = "";
		ipaddr = "";
		selectedGroups = new HashSet<Integer>();
		selectedDatabases = new HashSet<String>();
	}

	private String SQL_GROUP_MEMBERSHIPS = "SELECT g.groupname, " +
												  "(CASE (SELECT COUNT(*) " +
												  		   "FROM memberships m " +
												  		  "WHERE m.groupid = g.groupid " +
												  		  	"AND m.userid = (SELECT u.userid " +
												  		  					  "FROM users u " +
												  		  					 "WHERE u.username = ?)) " +
												   "WHEN 0 THEN 'no' " +
												   "ELSE 'yes' " +
												   "END) AS member " +
											 "FROM groups g ORDER BY 2 DESC, 1";
	protected WebResponse runOnce(HttpRequest request) {
		String operation = JUtility.valueOf(request.parameters.get(PARAMETER_OPERATION), "");
		StringWriter writer = new StringWriter();
		writer.write("<html>");
		writer.write("<head>");
		writer.write("<title>JSnap Web Console</title>");
		writer.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"/style.css\"/>");
		writer.write("<script type=\"text/javascript\">");
		writer.write("<!--\r\n");
		writer.write("function sort(name) {\r\n");
		writer.write("  var listbox = document.getElementById(name);\r\n");
		writer.write("  texts = new Array();\r\n");
		writer.write("  for(i = 0; i < listbox.length; ++i)\r\n");
		writer.write("    texts[i] = listbox.options[i].text;\r\n");
		writer.write("  texts.sort();\r\n");
		writer.write("  for(i = 0; i < listbox.length; i++) {\r\n");
		writer.write("    listbox.options[i].text = texts[i];\r\n");
		writer.write("    listbox.options[i].value = texts[i];\r\n");
		writer.write("  }\r\n");
		writer.write("}\r\n");
		writer.write("function transfer(from, to) {\r\n");
		writer.write("  var i;\r\n");
		writer.write("  var fromSelect = document.getElementById(from);\r\n");
		writer.write("  var toSelect = document.getElementById(to);\r\n");
		writer.write("  for (i = fromSelect.length - 1; i >= 0; i = i - 1) {\r\n");
		writer.write("    if (fromSelect.options[i].selected) {\r\n");
		writer.write("      var option = document.createElement('option');\r\n");
		writer.write("      option.text = fromSelect.options[i].text;\r\n");
		writer.write("      option.value = fromSelect.options[i].value;\r\n");
		writer.write("      try {\r\n");
		writer.write("        toSelect.add(option, null); // standards compliant; doesn't work in IE\r\n");
		writer.write("      } catch(ex) {\r\n");
		writer.write("        toSelect.add(option); // IE only\r\n");
		writer.write("      }\r\n");
		writer.write("      fromSelect.remove(i);\r\n");
		writer.write("    }\r\n");
		writer.write("  }\r\n");
		writer.write("  sort(to);\r\n");
		writer.write("}\r\n");
		writer.write("function selectAll() {\r\n");
		writer.write("  var memberOf = document.getElementById('member');\r\n");
		writer.write("  for(i = 0; i < memberOf.length; i++)\r\n");
		writer.write("    memberOf.options[i].selected = true;\r\n");
		writer.write("}\r\n");
		writer.write("// -->");
		writer.write("</script>");
		writer.write("</head>");
		writer.write("<body>");
		if (operation.equals(OPERATION_VIEW_GROUPS)) {
			String username = JUtility.valueOf(request.parameters.get(PARAMETER_USERNAME), "");
			int current = JUtility.valueOf(request.parameters.get(PARAMETER_CURRENT), 1);
			if (username.length() == 0) {
				writer.write("<p>A username is not provided.</p>");
			} else {
				try {
					Set<String> memberOf = new HashSet<String>();
					Set<String> notMemberOf = new HashSet<String>();
					DbInstance dbi = getDbInstance();
					try {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.STRING, username, false));
						DbResultSet result = dbi.createStatement(SQL_GROUP_MEMBERSHIPS, parameters, false).execute();
						try {
							while (result.next()) {
								String member = (String)result.get("member");
								String groupname = (String)result.get("groupname");
								if (member.equals("yes"))
									memberOf.add(groupname);
								else
									notMemberOf.add(groupname);
							}
						} finally {
							result.close();
						}
					} finally {
						dbi.close();
					}
					int size = Math.max(memberOf.size(), notMemberOf.size());
					size = Math.max(size, 4);
					size = Math.min(size, 8);
					// 4 <= size <= 8
					writer.write("<p>Modify group memberships for user <b>" + username + "</b>:</p>");
					writer.write("<form method=\"post\" action=\"main.do\">");
					writer.write("<input type=\"hidden\" name=\"page\" value=\"" + key() + "\"/>");
					writer.write("<input type=\"hidden\" name=\"operation\" value=\"updategroups\"/>");
					writer.write("<input type=\"hidden\" name=\"username\" value=\"" + username + "\"/>");
					writer.write("<input type=\"hidden\" name=\"current\" value=\"" + current + "\"/>");
					writer.write("<table class=\"darkgray\" cellpadding=\"4\">");
					writer.write("<tr><td align=\"center\">Member Of:</td><td></td><td align=\"center\">Not Member Of:</td></tr>");
					writer.write("<tr><td rowspan=\"2\" align=\"center\">");
					writer.write("<select id=\"member\" name=\"memberof\" multiple=\"true\" size=\"" + size + "\" style=\"width:200px\">");
					for (String groupname: memberOf)
						writer.write("<option value=\"" + groupname + "\">" + groupname + "</option>");
					writer.write("</select>");
					writer.write("<script type=\"text/javascript\">");
					writer.write("sort('member');");
					writer.write("</script>");
					writer.write("</td>");
					writer.write("<td><button class=\"button\" onclick=\"transfer('notmember','member');return false;\">&lt;</button></td>");
					writer.write("<td rowspan=\"2\" align=\"center\">");
					writer.write("<select id=\"notmember\" name=\"notmemberof\" multiple=\"true\" size=\"" + size + "\" style=\"width:200px\">");
					for (String groupname: notMemberOf)
						writer.write("<option value=\"" + groupname + "\">" + groupname + "</option>");
					writer.write("</select>");
					writer.write("<script type=\"text/javascript\">");
					writer.write("sort('notmember');");
					writer.write("</script>");
					writer.write("</td></tr>");
					writer.write("<tr><td><button class=\"button\" onclick=\"transfer('member','notmember');return false;\">&gt;</button></td></tr>");
					writer.write("<tr><td align=\"center\"><input class=\"button\" type=\"submit\" value=\"Save Changes\" onclick=\"selectAll();\"/></td><td></td>");
					writer.write("<td align=\"center\"><input class=\"button jqmClose\" type=\"submit\" value=\"Cancel\" onclick=\"return false;\"/></td></tr>");
					writer.write("</table>");
					writer.write("</form>");
				} catch (SqlException e) {
					e.log();
					writer.write("<p>" + e.getMessage() + "</p>");
				}
			}
		} else {
			writer.write("<p>Nothing to do for this operation: (" + operation + ")</p>");
		}
		writer.write("</body>");
		writer.write("</html>");
		writer.flush();
		JDocument doc;
		try {
			doc = JDocument.readFrom(new StringReader(writer.toString()));
		} catch (SAXException e) {
			doc = new JDocument("html");
			doc.appendTextNode("body", e.getMessage());
		} catch (IOException e) {
			doc = new JDocument("html");
			doc.appendTextNode("body", e.getMessage());
		}
		return new WebResponse(doc, null, Formatter.HTML, Formatter.DEFAULT);
	}

	protected void initialize() {
		mdalgAlgo = null;
		mdalgError = null;
		mdalgDifferent = 0;
		mdalgShow = false;
	}

	protected void appendPageData(JDocument doc) {
		doc.appendTextNodeWithAttrNoCheck(PARAMETER_USERNAME, username, new JPair<String, String>("selected", Boolean.toString(username.length() > 0)));
		groupList(doc);
		databaseList(doc);
		doc.appendNodeHierarchyNoCheck(ACCOUNT_STATUS_ROOT, ACCOUNT_STATUS_NODES, new boolean[]{active, expired, locked}, new String[] {"", "", ""});
		doc.appendNodeHierarchyNoCheck(ADMINISTRATIVE_RIGHTS_ROOT, ADMINISTRATIVE_RIGHTS_NODES, new boolean[]{adminAsUser, adminFromGroup, ordinaryUser}, new String[] {"", "", ""});
		doc.appendTextNodeWithAttrNoCheck(PARAMETER_IPADDRESS, ipaddr, new JPair<String, String>("selected", Boolean.toString(ipaddr.length() > 0)));
		messageDigestAlgoInformation(doc);
	}

	protected boolean needsRefresh(HttpRequest request) {
		return (request.parameters.get(PARAMETER_REFRESH) != null);
	}

	protected void refresh(HttpRequest request) {
		// Username:
		username = "";
		String usernamestate = request.parameters.get(PARAMETER_USERNAME + STATE);
		if (usernamestate != null && usernamestate.equals(ON))
			username = JUtility.valueOf(request.parameters.get(PARAMETER_USERNAME), "").trim();
		// Database list:
		selectedDatabases.clear();
		String dbstate = request.parameters.get(PARAMETER_DATABASE + STATE);
		if (dbstate != null && dbstate.equals(ON)) {
			Set<String> databases = request.parameters.getAll(PARAMETER_DATABASE);
			if (databases != null) {
				for (String database: databases)
					selectedDatabases.add(database);
			}
		}
		// Group list:
		selectedGroups.clear();
		String groupstate = request.parameters.get(PARAMETER_GROUP + STATE);
		if (groupstate != null && groupstate.equals(ON)) {
			Set<String> groups = request.parameters.getAll(PARAMETER_GROUP);
			if (groups != null) {
				for (String group: groups) {
					int groupid = JUtility.valueOf(group, 0);
					if (groupid > 0)
						selectedGroups.add(groupid);
				}
			}
		}
		// Account status:
		active = false;
		expired = false;
		locked = false;
		String statusstate = request.parameters.get(PARAMETER_ACCT_STATUS + STATE);
		if (statusstate != null && statusstate.equals(ON)) {
			active = (request.parameters.get(PARAMETER_ACCT_STATUS_ACTIVE) != null);
			expired = (request.parameters.get(PARAMETER_ACCT_STATUS_EXPIRED) != null);
			locked = (request.parameters.get(PARAMETER_ACCT_STATUS_LOCKED) != null);
		}
		// Administrative rights:
		adminAsUser = false;
		adminFromGroup = false;
		ordinaryUser = false;
		String rightsstate = request.parameters.get(PARAMETER_ADMIN_RIGHTS + STATE);
		if (rightsstate != null && rightsstate.equals(ON)) {
			adminAsUser = (request.parameters.get(PARAMETER_ADMIN_RIGHTS_USER) != null);
			adminFromGroup = (request.parameters.get(PARAMETER_ADMIN_RIGHTS_GROUP) != null);
			ordinaryUser = (request.parameters.get(PARAMETER_ADMIN_RIGHTS_NONE) != null);
		}
		// IP address:
		ipaddr = "";
		String ipaddrstate = request.parameters.get(PARAMETER_IPADDRESS + STATE);
		if (ipaddrstate != null && ipaddrstate.equals(ON))
			ipaddr = JUtility.valueOf(request.parameters.get(PARAMETER_IPADDRESS), "").trim();
		// Different message digest algorithm query:
		String operation = JUtility.valueOf(request.parameters.get(PARAMETER_OPERATION), "");
		qyalgodiff = operation.equals(OPERATION_QUERY_DIFFALGO);
	}

	protected boolean operation(JDocument doc, HttpRequest request) {
		boolean refresh = true;
		String operation = JUtility.valueOf(request.parameters.get(PARAMETER_OPERATION), OPERATION_LIST_ALL);
		if (operation.equals(OPERATION_ALGO_MODIFY)) {
			messageDigestAlgoModify(request);
			refresh = false;
		} else if (operation.equals(OPERATION_USER_CREATE)) {
			userCreate(doc, request);
		} else if (operation.equals(OPERATION_USER_DELETE)) {
			userDelete(doc, request);
		} else if (operation.equals(OPERATION_GRANT_PRIVILEGES)) {
			modifyPrivileges(doc, request, true);
		} else if (operation.equals(OPERATION_REVOKE_PRIVILEGES)) {
			modifyPrivileges(doc, request, false);
		} else if (operation.equals(OPERATION_ACCOUNT_LOCKOUT)) {
			userLockout(doc, request, true);
		} else if (operation.equals(OPERATION_ACCOUNT_UNLOCK)) {
			userLockout(doc, request, false);
		} else if (operation.equals(OPERATION_EXPIRE_PASSWORDS)) {
			expirePasswords(doc, request);
		} else if (operation.equals(OPERATION_UPDATE_GROUPS)) {
			updateGroups(doc, request);
			refresh = false;
		} else {
			refresh = false;
		}
		return refresh;
	}

	private static final String SQL_DIGEST_ALGO = "SELECT p.value AS \"value\" FROM parameters p WHERE p.name = ?";
	private String messageDigestAlgo() {
		String algo = null;
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				parameters.add(new DbParam(DbParam.STRING, Authenticate.PASSWORD_MDALG_PARAMNAME, false));
				DbStatement statement = dbi.createStatement(SQL_DIGEST_ALGO, parameters, false);
				DbResultSet result = statement.execute();
				try {
					while (algo == null && result.next())
						algo = (String)result.get("value");
				} finally {
					result.close();
				}
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
		}
		return algo;
	}

	private static final String SQL_DIGEST_ALGO_DIFFERENT = "SELECT COUNT(*) AS \"different\" " +
	  														  "FROM users u " +
	  														 "WHERE u.pwdexpired = FALSE " +
	  														   "AND u.mdalg <> ?";
	private void messageDigestAlgoInformation(JDocument doc) {
		mdalgAlgo = messageDigestAlgo();
		if (mdalgAlgo == null) {
			if (mdalgError == null)
				mdalgError = Authenticate.PASSWORD_MDALG_ERROR;
		} else {
			try {
				DbInstance dbi = getDbInstance();
				try {
					ArrayList<DbParam> parameters = new ArrayList<DbParam>();
					parameters.add(new DbParam(DbParam.STRING, mdalgAlgo, false));
					DbStatement statement = dbi.createStatement(SQL_DIGEST_ALGO_DIFFERENT, parameters, false);
					DbResultSet result = statement.execute();
					try {
						while (result.next())
							mdalgDifferent = JUtility.intValueOf(result.get("different"), 0);
					} finally {
						result.close();
					}
				} finally {
					dbi.close();
				}
			} catch (SqlException e) {
				e.log(); // logged and skipped.
				if (mdalgError == null)
					mdalgError = e.getMessage();
			}
		}
		if (mdalgError != null)
			mdalgShow = true;
		doc.appendNodeHierarchyNoCheck(DIGEST_ALGO_ROOT, DIGEST_ALGO_NODES, new String[]{mdalgAlgo, Integer.toString(mdalgDifferent), mdalgError, Boolean.toString(mdalgShow)});
	}

	private static final String SQL_INSERT_DIGEST_ALGO = "INSERT INTO parameters(name, value) VALUES(?, ?)";
	private static final String SQL_UPDATE_DIGEST_ALGO = "UPDATE parameters SET value = ? WHERE name = ?";
	private void messageDigestAlgoModify(HttpRequest request) {
		String algo = JUtility.valueOf(request.parameters.get(PARAMETER_ALGORITHM), "").trim().toUpperCase();
		if (algo.length() == 0) {
			mdalgError = "A message digest algorithm is not specified.";
		} else {
			boolean found = true;
			if (algo.equals(AuthenticationPolicy.NO_DIGEST) == false) {
				try {
					MessageDigest.getInstance(algo);
				} catch (NoSuchAlgorithmException e) {
					mdalgError = algo + " is not a known message digest algorithm.";
					found = false;
				}
			}
			if (found) {
				try {
					DbInstance dbi = getDbInstance();
					try {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.STRING, algo, false));
						parameters.add(new DbParam(DbParam.STRING, Authenticate.PASSWORD_MDALG_PARAMNAME, false));
						DbStatement statement = dbi.createStatement(SQL_UPDATE_DIGEST_ALGO, parameters, false);
						DbResultSet result = statement.execute();
						try {
							if (result.affectedRows() <= 0) {
								parameters.clear();
								parameters.add(new DbParam(DbParam.STRING, Authenticate.PASSWORD_MDALG_PARAMNAME, false));
								parameters.add(new DbParam(DbParam.STRING, algo, false));
								statement = dbi.createStatement(SQL_INSERT_DIGEST_ALGO, parameters, false);
								statement.execute().close();
							}
						} finally {
							result.close();
						}
						dbi.commit();
						mdalgShow = true;
					} finally {
						dbi.close();
					}
				} catch (SqlException e) {
					e.log(); // logged and skipped.
					if (mdalgError == null)
						mdalgError = e.getMessage();
				}
			}
		}
	}

	private static final String SQL_USERNAME_UNIQUE = "SELECT u.userid FROM users u WHERE u.username = ?";
	private boolean allowCreate(String username) {
		boolean unique = true;
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				parameters.add(new DbParam(DbParam.STRING, username, false));
				DbStatement statement = dbi.createStatement(SQL_USERNAME_UNIQUE, parameters, false);
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

	private static final String SQL_CREATE_USER = "INSERT INTO users(username, password, mdalg, admin, pwdexpired, actlocked) VALUES(?, ?, ?, ?, ?, ?)";
	private void userCreate(JDocument doc, HttpRequest request) {
		boolean success = false;
		String createUsername = JUtility.valueOf(request.parameters.get(PARAMETER_CREATE_NAME), "").trim();
		String createPassword = JUtility.valueOf(request.parameters.get(PARAMETER_CREATE_PASSWORD), "").trim();
		String createError = null;
		boolean createAdmin = (request.parameters.get(PARAMETER_CREATE_ADMIN) != null);
		boolean createExpired = (request.parameters.get(PARAMETER_CREATE_EXPIRED) != null);
		boolean createLocked = (request.parameters.get(PARAMETER_CREATE_LOCKED) != null);
		if (createUsername.length() == 0) {
			createError = "Username cannot be left blank.";
		} else if (allowCreate(createUsername) == false) {
			createError = "A user named " + createUsername + " already exists.";
		} else {
			try {
				String mdalg = JUtility.valueOf(messageDigestAlgo(), AuthenticationPolicy.NO_DIGEST);
				String encryptedPassword;
				try {
					MessageDigest md = MessageDigest.getInstance(mdalg);
					encryptedPassword = (mdalg.equals(AuthenticationPolicy.NO_DIGEST) ? createPassword : JUtility.toHexString(md.digest(createPassword.getBytes())));
				} catch (NoSuchAlgorithmException e) {
					mdalg = AuthenticationPolicy.NO_DIGEST;
					encryptedPassword = createPassword;
				}
				DbInstance dbi = getDbInstance();
				try {
					ArrayList<DbParam> parameters = new ArrayList<DbParam>();
					parameters.add(new DbParam(DbParam.STRING, createUsername, false));
					parameters.add(new DbParam(DbParam.STRING, encryptedPassword, false));
					parameters.add(new DbParam(DbParam.STRING, mdalg, false));
					parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(createAdmin), false));
					parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(createExpired), false));
					parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(createLocked), false));
					dbi.createStatement(SQL_CREATE_USER, parameters, false).execute().close();
					dbi.commit();
					success = true;
				} finally {
					dbi.close();
				}
			} catch (SqlException e) {
				e.log(); // logged and skipped.
				createError = e.getMessage();
			}
		}
		if (success)
			doc.appendNodeHierarchy(USER_CREATE_ROOT, USER_CREATE_NODES, new String[]{"User " + createUsername + " is created successfully."});
		else
			doc.appendNodeHierarchy(USER_CREATE_ROOT, USER_CREATE_NODES, new String[]{"",
																				  createError,
																				  createUsername,
																				  createPassword,
																				  Boolean.toString(createAdmin),
																				  Boolean.toString(createExpired),
																				  Boolean.toString(createLocked)});
	}

	private static final String SQL_DELETE_MEMBERSHIPS = "DELETE FROM memberships WHERE userid = (SELECT u.userid FROM users u WHERE u.username = ?)";
	private static final String SQL_DELETE_USER = "DELETE FROM users WHERE username = ?";
	private void userDelete(JDocument doc, HttpRequest request) {
		String operationMessage = null, operationError = null;
		try {
			Set<String> selected = getSelectedUsers(request);
			if (selected.size() == 0) {
				operationError = "No users were selected.";
			} else {
				DbInstance dbi = getDbInstance();
				try {
					for (String username: selected) {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.STRING, username, false));
						dbi.createStatement(SQL_DELETE_MEMBERSHIPS, parameters, false).execute().close();
						dbi.createStatement(SQL_DELETE_USER, parameters, false).execute().close();
					}
					boolean activeAdminExists = activeAdminExists(dbi);
					if (activeAdminExists) {
						dbi.commit();
						if (selected.size() == 1)
							operationMessage = "Deleted a single user.";
						else
							operationMessage = "Deleted " + selected.size() + " users.";
					} else {
						dbi.rollback();
						operationError = "The requested operation would remove all active administrators from the system and therefore is not allowed.";
					}
				} finally {
					dbi.close();
				}
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			operationError = e.getMessage();
		}
		doc.appendNodeHierarchyNoCheck(OPERATION_ROOT, OPERATION_NODES, new String[]{operationMessage, operationError});
	}

	private static final String SQL_UPDATE_ADMIN = "UPDATE users SET admin = ? WHERE username = ?";
	private void modifyPrivileges(JDocument doc, HttpRequest request, boolean grant) {
		String operationMessage = null, operationError = null;
		try {
			Set<String> selected = getSelectedUsers(request);
			if (selected.size() == 0) {
				operationError = "No users were selected.";
			} else {
				DbInstance dbi = getDbInstance();
				try {
					for (String username: selected) {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(grant), false));
						parameters.add(new DbParam(DbParam.STRING, username, false));
						dbi.createStatement(SQL_UPDATE_ADMIN, parameters, false).execute().close();
					}
					boolean activeAdminExists = activeAdminExists(dbi);
					if (grant || activeAdminExists) {
						dbi.commit();
						if (selected.size() == 1) {
							if (grant)
								operationMessage = "Granted administrative privileges to a single user.";
							else
								operationMessage = "Revoked administrative privileges from a single user.";
						} else {
							if (grant)
								operationMessage = "Granted administrative privileges to " + selected.size() + " users.";
							else
								operationMessage = "Revoked administrative privileges from " + selected.size() + " users.";
						}
					} else {
						dbi.rollback();
						operationError = "The requested operation would remove all active administrators from the system and therefore is not allowed.";
					}
				} finally {
					dbi.close();
				}
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			operationError = e.getMessage();
		}
		doc.appendNodeHierarchyNoCheck(OPERATION_ROOT, OPERATION_NODES, new String[]{operationMessage, operationError});
	}

	private static final String SQL_LOCKOUT_USER = "UPDATE users SET actlocked = ? WHERE username = ?";
	private void userLockout(JDocument doc, HttpRequest request, boolean lock) {
		String operationMessage = null, operationError = null;
		try {
			Set<String> selected = getSelectedUsers(request);
			if (selected.size() == 0) {
				operationError = "No users were selected.";
			} else {
				DbInstance dbi = getDbInstance();
				try {
					for (String username: selected) {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.BOOLEAN, Boolean.toString(lock), false));
						parameters.add(new DbParam(DbParam.STRING, username, false));
						dbi.createStatement(SQL_LOCKOUT_USER, parameters, false).execute().close();
					}
					boolean activeAdminExists = activeAdminExists(dbi);
					if (lock == false || activeAdminExists) {
						dbi.commit();
						if (selected.size() == 1)
							operationMessage = (lock ? "Locked out" : "Unlocked") + " a single account.";
						else
							operationMessage = (lock ? "Locked out " : "Unlocked ") + selected.size() + " accounts.";
					} else {
						dbi.rollback();
						operationError = "The requested operation would remove all active administrators from the system and therefore is not allowed.";
					}
				} finally {
					dbi.close();
				}
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			operationError = e.getMessage();
		}
		doc.appendNodeHierarchyNoCheck(OPERATION_ROOT, OPERATION_NODES, new String[]{operationMessage, operationError});
	}

	private static final String SQL_EXPIRE_PASSWORD = "UPDATE users SET pwdexpired = TRUE WHERE username = ?";
	private void expirePasswords(JDocument doc, HttpRequest request) {
		String operationMessage = null, operationError = null;
		try {
			Set<String> selected = getSelectedUsers(request);
			if (selected.size() == 0) {
				operationError = "No users were selected.";
			} else {
				DbInstance dbi = getDbInstance();
				try {
					for (String username: selected) {
						ArrayList<DbParam> parameters = new ArrayList<DbParam>();
						parameters.add(new DbParam(DbParam.STRING, username, false));
						dbi.createStatement(SQL_EXPIRE_PASSWORD, parameters, false).execute().close();
					}
					dbi.commit();
					if (selected.size() == 1)
						operationMessage = "Password of a single user is now expired.";
					else
						operationMessage = "Passwords of " + selected.size() + " users are now expired.";
				} finally {
					dbi.close();
				}
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			operationError = e.getMessage();
		}
		doc.appendNodeHierarchyNoCheck(OPERATION_ROOT, OPERATION_NODES, new String[]{operationMessage, operationError});
	}

	private String SQL_GET_USERID = "SELECT u.userid FROM users u WHERE u.username = ?";
	private String SQL_GET_GROUPID = "SELECT g.groupid FROM groups g WHERE g.groupname = ?";
	private String SQL_DELETE_MEMBERSHIPS_2 = "DELETE FROM memberships WHERE userid = ?";
	private String SQL_MAKE_MEMBERSHIP = "INSERT INTO memberships(groupid, userid) VALUES(?, ?)";
	private void updateGroups(JDocument doc, HttpRequest request) {
		String operationMessage = null, operationError = null;
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		// First of all, check if the username in the request is a valid one. Obtain the userid
		// in order to continue with the operation.
		String username = JUtility.valueOf(request.parameters.get(PARAMETER_USERNAME), "");
		int userid = 0;
		try {
			DbInstance dbi = getDbInstance();
			try {
				parameters.add(new DbParam(DbParam.STRING, username, false));
				DbResultSet result = dbi.createStatement(SQL_GET_USERID, parameters, false).execute();
				try {
					while (userid == 0 && result.next())
						userid = JUtility.intValueOf(result.get("userid"), 0);
				} finally {
					result.close();
				}
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log();
			operationError = e.getMessage();
		}
		if (userid > 0) {
			// Username is valid, corresponding userid is found.
			try {
				Set<String> memberOf = request.parameters.getAll(PARAMETER_MEMBEROF);
				DbInstance dbi = getDbInstance();
				try {
					// Initially delete the previous memberships.
					parameters.clear();
					parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(userid), false));
					dbi.createStatement(SQL_DELETE_MEMBERSHIPS_2, parameters, false).execute().close();
					// Then make the requested memberships.
					if (memberOf != null) {
						for (String groupname: memberOf) {
							// Find the groupid for the specified groupname.
							int groupid = 0;
							parameters.clear();
							parameters.add(new DbParam(DbParam.STRING, groupname, false));
							DbResultSet result = dbi.createStatement(SQL_GET_GROUPID, parameters, false).execute();
							try {
								while (groupid == 0 && result.next())
									groupid = JUtility.intValueOf(result.get("groupid"), 0);
							} finally {
								result.close();
							}
							// If groupname is valid, a groupid is found; make the membership.
							if (groupid > 0) {
								parameters.clear();
								parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(groupid), false));
								parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(userid), false));
								dbi.createStatement(SQL_MAKE_MEMBERSHIP, parameters, false).execute().close();
							}
						}
					}
					// Check if an administrator exists in the system after the operation;
					// if not, changes will be rolled back.
					boolean activeAdminExists = activeAdminExists(dbi);
					if (activeAdminExists) {
						dbi.commit();
						int s = (memberOf == null ? 0 : memberOf.size());
						if (s == 0)
							operationMessage = username + " is not member of any groups from now on.";
						else if (s == 1)
							operationMessage = username + " is now member of a single group.";
						else
							operationMessage = username + " is now member of " + s + " groups.";
					} else {
						dbi.rollback();
						operationError = "The requested operation would remove all active administrators from the system and therefore is not allowed.";
					}
				} finally {
					dbi.close();
				}
			} catch (SqlException e) {
				e.log();
				operationError = e.getMessage();
			}
		} else {
			operationError = "Specified user (" + username + ") does not exist.";
		}
		doc.appendNodeHierarchyNoCheck(OPERATION_ROOT, OPERATION_NODES, new String[]{operationMessage, operationError});
	}

	private Set<String> getSelectedUsers(HttpRequest request) throws SqlException {
		String selection = JUtility.valueOf(request.parameters.get(PARAMETER_SELECTION), "");
		Set<String> selected = new HashSet<String>();
		if (selection.equals(PARAMETER_SELECTION_ALL)) {
			result.positionAt(0);
			while (result.next()) {
				String username = (String)result.get("name");
				selected.add(username);
			}
		} else if (selection.equals(PARAMETER_SELECTION_SELECTED)) {
			Set<String> listedUsers = request.parameters.getAll(PARAMETER_SELECTION_LISTEDUSER);
			for (String username: listedUsers) {
				boolean checked = (request.parameters.get(username) != null);
				if (checked)
					selected.add(username);
			}
		}
		return selected;
	}

	protected DbResultSet create(DbInstance dbi) throws SqlException {
		final String administratorAsUser = "yes (as user)";
		final String administratorFromGroup = "yes (as group member)";
		final String noPrivilege = "no";
		// Create body of the selection (SELECT) and the joined tables (FROM).
		String sql = "SELECT DISTINCT u.userid AS \"id\", u.username AS \"name\", " +
							"(CASE u.admin " +
							   "WHEN true THEN '" + administratorAsUser + "' " +
							   "ELSE (CASE " +
							   		   "WHEN (SELECT COUNT(*) " +
							   		   		   "FROM users s, " +
							   		   		   		"groups g, " +
							   		   		   		"memberships m " +
							   		   		  "WHERE u.userid = s.userid " +
							   		   		  	"AND s.userid = m.userid " +
							   		   		  	"AND g.groupid = m.groupid " +
							   		   		  	"AND g.administrative = TRUE) > 0 THEN '" + administratorFromGroup + "' " +
							   		   "ELSE '" + noPrivilege + "' " +
							   		 "END) " +
							 "END) AS \"admin\", " +
							"(CASE u.pwdexpired WHEN true THEN 'yes' ELSE 'no' END) AS \"expired\", " +
							"(CASE u.actlocked WHEN true THEN 'yes' ELSE 'no' END) AS \"locked\", " +
							"1 AS \"counter\" " + // Required for AbstractWebMultiPage to work correctly.
		               "FROM users u";
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		if (qyalgodiff) {
			sql += " WHERE u.pwdexpired = FALSE " +
					  "AND u.mdalg <> (SELECT p.value " +
					  					"FROM parameters p " +
					  				   "WHERE p.name = ?)";
			parameters.add(new DbParam(DbParam.STRING, Authenticate.PASSWORD_MDALG_PARAMNAME, false));
		} else {
			// Perform joins if necessary.
			boolean membershipsJoin = (selectedGroups.size() > 0);
			boolean authhistoryJoin = (selectedDatabases.size() > 0 || ipaddr.length() > 0);
			if (membershipsJoin)
				sql += ", memberships m";
			if (authhistoryJoin)
				sql += ", authhistory a";
			// Create the where clause (WHERE).
			String where = "";
			if (membershipsJoin) {
				where += "u.userid = m.userid AND (";
				boolean first = true;
				for (int group: selectedGroups) {
					if (first == false)
						where += " OR ";
					where += "m.groupid = ?";
					parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(group), false));
					first = false;
				}
				where += ")";
			}
			if (authhistoryJoin) {
				if (where.length() > 0)
					where += " AND ";
				where += "u.username = a.username";
				if (ipaddr.length() > 0) {
					where += " AND a.ipaddress LIKE ?";
					parameters.add(new DbParam(DbParam.STRING, ipaddr.replace('*', '%'), false));
				}
				if (selectedDatabases.size() > 0) {
					where += " AND (";
					boolean first = true;
					for (String database: selectedDatabases) {
						if (first == false)
							where += " OR ";
						where += "(a.dbname = ? AND a.type = ?)";
						parameters.add(new DbParam(DbParam.STRING, database, false));
						parameters.add(new DbParam(DbParam.STRING, "auth", false));
						first = false;
					}
					where += ")";
				}
			}
			if (username.length() > 0) {
				if (where.length() > 0)
					where += " AND ";
				where += "u.username LIKE ?";
				parameters.add(new DbParam(DbParam.STRING, username.replace('*', '%'), false));
			}
			/*   #   a   e   l
			 * -----------------
			 *   0   0   0   0
			 *   1   0   0   1
			 *   2   0   1   0
			 *   3   0   1   1
			 *   4   1   0   0
			 *   5   1   0   1
			 *   6   1   1   0
			 *   7   1   1   1  */ // status is an integer between 0 and 7 by definition.
			int status = (active ? 4 : 0) + (expired ? 2 : 0) + (locked ? 1 : 0);
			if (status >= 1 && status <= 6) {
				if (where.length() > 0) 
					where += " AND ";
				switch (status) {
				// locked = actlocked
				case 1: where += "u.actlocked = TRUE"; break;
				// expired = pwdexpired
				case 2: where += "u.pwdexpired = TRUE"; break;
				// expired + locked = pwdexpired + actlocked
				case 3: where += "(u.pwdexpired = TRUE OR u.actlocked = TRUE)"; break;
				// active = !pwdexpired . !actlocked
				case 4: where += "u.pwdexpired = FALSE AND u.actlocked = FALSE"; break;
				// active + locked = !pwdexpired . !actlocked + actlocked
				//                 = !pwdexpired + actlocked
				case 5: where += "(u.pwdexpired = FALSE OR u.actlocked = TRUE)"; break;
				// active + expired = !pwdexpired . !actlocked + pwdexpired
				//                  = pwdexpired + !actlocked
				case 6: where += "(u.pwdexpired = TRUE OR u.actlocked = FALSE)"; break;
				// active + expired + locked = !pwdexpired . !actlocked + pwdexpired + actlocked
				//                           = pwdexpired + !actlocked + actlocked
				//                           = 1
				// No need to append anything to the where clause for num == 7.
				}
			}
			if (where.length() > 0)
				sql += " WHERE " + where;
			/*   #   u   g   o
			 * -----------------
			 *   0   0   0   0
			 *   1   0   0   1
			 *   2   0   1   0
			 *   3   0   1   1
			 *   4   1   0   0
			 *   5   1   0   1
			 *   6   1   1   0
			 *   7   1   1   1  */ // rights is an integer between 0 and 7 by definition.
			int rights = (adminAsUser ? 4 : 0) + (adminFromGroup ? 2 : 0) + (ordinaryUser ? 1 : 0);
			if (rights >= 1 && rights <= 6) {
				sql = "SELECT v.* FROM (" + sql + ") v WHERE ";
				switch (rights) {
				case 1: sql += "v.\"admin\" = '" + noPrivilege + "'"; break;
				case 2: sql += "v.\"admin\" = '" + administratorFromGroup + "'"; break;
				case 3: sql += "v.\"admin\" IN ('" + noPrivilege + "', '" + administratorFromGroup + "')"; break;
				case 4: sql += "v.\"admin\" = '" + administratorAsUser + "'"; break;
				case 5: sql += "v.\"admin\" IN ('" + noPrivilege + "', '" + administratorAsUser + "')"; break;
				case 6: sql += "v.\"admin\" IN ('" + administratorFromGroup + "', '" + administratorAsUser + "')"; break;
				}
			}
		}
		sql +=  " ORDER BY 3 DESC, 2";
		// Run the SQL statement.
		DbStatement statement = dbi.createStatement(sql, parameters, true);
		result = statement.execute();
		return result;
	}

	private static final String SQL_GROUP_LIST = "SELECT g.groupid AS \"groupid\", g.groupname AS \"groupname\" FROM groups g ORDER BY g.groupname";
	private void groupList(JDocument doc) {
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<JPair<Integer, String>> groups = new ArrayList<JPair<Integer,String>>();
				ArrayList<Boolean> selected = new ArrayList<Boolean>();
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				DbStatement statement = dbi.createStatement(SQL_GROUP_LIST, parameters, false);
				DbResultSet result = statement.execute();
				try {
					while (result.next()) {
						int groupid = JUtility.intValueOf(result.get("groupid"), 0);
						if (groupid > 0) {
							String groupname = (String)result.get("groupname");
							groups.add(new JPair<Integer, String>(groupid, groupname));
							selected.add(selectedGroups.contains(groupid));
						}
					}
				} finally {
					result.close();
				}
				doc.appendNodeHierarchyNoCheck(GROUPS_ROOT, GROUPS_ITEM, GROUP_ID, GROUP_NAME, groups, selected);
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			doc.appendEmptyNode(GROUPS_ROOT);
		}
	}

	private static final String SQL_DATABASE_LIST = "SELECT DISTINCT a.dbname AS \"database\" FROM authhistory a ORDER BY a.dbname";
	private void databaseList(JDocument doc) {
		try {
			DbInstance dbi = getDbInstance();
			try {
				ArrayList<String> databases = new ArrayList<String>();
				ArrayList<DbParam> parameters = new ArrayList<DbParam>();
				DbStatement statement = dbi.createStatement(SQL_DATABASE_LIST, parameters, false);
				DbResultSet result = statement.execute();
				try {
					while (result.next()) {
						String database = (String)result.get("database");
						databases.add(database);
					}
				} finally {
					result.close();
				}
				String[] values = new String[databases.size()];
				databases.toArray(values);
				String[] names = new String[databases.size()];
				boolean[] selected = new boolean[databases.size()];
				for (int i = 0; i < names.length; ++i) {
					names[i] = DATABASES_ITEM;
					selected[i] = selectedDatabases.contains(values[i]);
				}
				doc.appendNodeHierarchyNoCheck(DATABASES_ROOT, names, selected, values);
			} finally {
				dbi.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
			doc.appendEmptyNode(DATABASES_ROOT);
		}
	}

	private static final String SQL_ACTIVE_ADMIN = "SELECT u.userid " +
	   												 "FROM users u " +
	   												"WHERE u.admin = TRUE " +
	   												  "AND u.actlocked = FALSE " +
	   												"UNION " +
	   											   "SELECT u.userid " +
	   											     "FROM users u, " +
	   											     	  "groups g, " +
	   											     	  "memberships m " +
	   											    "WHERE u.userid = m.userid " +
	   											      "AND g.groupid = m.groupid " +
	   											      "AND g.administrative = TRUE " +
	   											      "AND u.actlocked = FALSE";
	public static boolean activeAdminExists(DbInstance dbi) {
		boolean adminExists = false;
		try {
			ArrayList<DbParam> parameters = new ArrayList<DbParam>();
			DbStatement statement = dbi.createStatement(SQL_ACTIVE_ADMIN, parameters, false);
			DbResultSet result = statement.execute();
			try {
				while (adminExists == false && result.next())
					adminExists = true;
			} finally {
				result.close();
			}
		} catch (SqlException e) {
			e.log(); // logged and skipped.
		}
		return adminExists;
	}
}
