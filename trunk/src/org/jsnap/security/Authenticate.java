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

package org.jsnap.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.db.base.Dbregistry;
import org.jsnap.exception.db.SqlException;
import org.jsnap.exception.security.LoginFailedException;
import org.jsnap.exception.security.PasswordManagementException;
import org.jsnap.exception.security.PasswordRenewalException;
import org.jsnap.exception.security.PasswordManagementException.Reason;
import org.jsnap.http.pages.AuthenticationStats;
import org.jsnap.http.pages.WebPage;
import org.jsnap.util.JUtility;

public class Authenticate implements AuthenticationPolicy {
	public static final String PASSWORD_MDALG_PARAMNAME = "password-mdalg";
	public static final String PASSWORD_MDALG_ERROR = "A program-wide message digest algorithm for user passwords is not specified";

	protected String authKey() {
		return "auth";
	}

	protected String renewKey() {
		return "renew";
	}

	private WebPage authenticationStats = null;
	private String ownerName = null;

	public void setOwnerName(String dbname) {
		ownerName = dbname;
	}

	public WebPage[] getPages() {
		if (authenticationStats == null) {
			if (ownerName != null)
				authenticationStats = new AuthenticationStats(ownerName);
			else
				return new WebPage[]{};
		}
		return new WebPage[]{ authenticationStats };
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname) throws LoginFailedException {
		return authenticateWrapper(dbi, credentials, dbname, 0);
	}

	public User authenticate(DbInstance dbi, Credentials credentials, String dbname, long timeout) throws LoginFailedException {
		long tryUntil = System.currentTimeMillis() + timeout;
		return authenticateWrapper(dbi, credentials, dbname, tryUntil);
	}

	private User authenticateWrapper(DbInstance dbi, Credentials credentials, String dbname, long tryUntil) throws LoginFailedException {
		LoginFailedException failed = null;
		User user = new User(credentials);
		try {
			user = authenticateInternal(dbi, credentials, tryUntil);
		} catch (LoginFailedException e) {
			failed = e;
		}
		log(dbi, user, dbname, authKey(), 0, (failed != null ? failed.reason : Reason.SUCCESS));
		if (failed != null)
			throw failed;
		return user;
	}

	private static final String SQL_RETRIEVE_USER = "SELECT userid, " +
														   "password, " +
														   "mdalg, " +
														   "admin, " +
														   "actlocked, " +
														   "pwdexpired " +
													  "FROM users " +
													 "WHERE username = ?";
	private User authenticateInternal(DbInstance dbi, Credentials credentials, long tryUntil) throws LoginFailedException {
		DbResultSet result = null;
		try {
			// Check user's credentials.
			ArrayList<DbParam> parameters = new ArrayList<DbParam>();
			parameters.add(new DbParam(DbParam.STRING, credentials.username, false));
			DbStatement statement = dbi.createStatement(SQL_RETRIEVE_USER, parameters, false);
			if (tryUntil > 0) {
				long remaining = tryUntil - System.currentTimeMillis();
				statement.setTimeout(remaining);
			}
			result = statement.execute();
			boolean userExists = false;
			int userid = 0;
			String password = null, mdalg = null;
			boolean actlocked = false, pwdexpired = false, admin = false;
			while (userExists == false && result.next()) {
				userid = JUtility.intValueOf(result.get("userid"), 0);
				if (userid > 0) {
					userExists = true;
					password = (String)result.get("password");
					mdalg = (String)result.get("mdalg");
					admin = (Boolean)result.get("admin");
					actlocked = (Boolean)result.get("actlocked");
					pwdexpired = (Boolean)result.get("pwdexpired");
				}
			}
			if (userExists == false)
				throw new LoginFailedException(Reason.NO_SUCH_USER, "User " + credentials.username + " does not exist");
			boolean plain = false, passwordIncorrect = false;
			String provided;
			if (mdalg.equals(AuthenticationPolicy.NO_DIGEST)) {
				provided = credentials.password;
				plain = true;
			} else {
				MessageDigest md = MessageDigest.getInstance(mdalg);
				provided = JUtility.toHexString(md.digest(credentials.password.getBytes()));
			}
			if (plain)
				passwordIncorrect |= (password.equals(provided) == false);
			else
				passwordIncorrect |= (password.equalsIgnoreCase(provided) == false);
			if (passwordIncorrect)
				throw new LoginFailedException(Reason.INCORRECT_PASSWORD, "Supplied password is incorrect");
			if (actlocked)
				throw new LoginFailedException(Reason.LOCKED_OUT, "Account is locked out");
			if (pwdexpired)
				throw new LoginFailedException(Reason.RENEW_PASSWORD, "User " + credentials.username + " must first renew his/her password");
			// Check if the user inherits administrator privileges with a group membership.
			if (admin == false)
				admin = memberOfAnAdminGroup(dbi, userid, tryUntil);
			return new User(credentials, (admin ? UserType.ADMINISTRATOR : UserType.USER));
		} catch (SqlException e) {
			throw new LoginFailedException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new LoginFailedException(e);
		} finally {
			if (result != null)
				result.close();
		}
	}

	private static final String SQL_ADMIN_GROUP_MEMBER = "SELECT g.administrative " +
														   "FROM groups g, " +
														   		"memberships m " +
														  "WHERE g.groupid = m.groupid " +
														    "AND g.administrative = true " +
														    "AND m.userid = ?";
	private boolean memberOfAnAdminGroup(DbInstance dbi, int userid, long tryUntil) throws SqlException {
		boolean admin = false;
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(userid), false));
		DbStatement statement = dbi.createStatement(SQL_ADMIN_GROUP_MEMBER, parameters, false);
		if (tryUntil > 0) {
			long remaining = tryUntil - System.currentTimeMillis();
			statement.setTimeout(remaining);
		}
		DbResultSet result = statement.execute();
		try {
			while (admin == false && result.next())
				admin |= (Boolean)result.get("administrative");
		} finally {
			result.close();
		}
		return admin;
	}

	public User changePassword(DbInstance dbi, Credentials credentials, String newPassword) throws PasswordManagementException {
		return changePasswordWrapper(dbi, credentials, newPassword, 0);
	}

	public User changePassword(DbInstance dbi, Credentials credentials, String newPassword, long timeout) throws PasswordManagementException {
		long tryUntil = System.currentTimeMillis() + timeout;
		return changePasswordWrapper(dbi, credentials, newPassword, tryUntil);
	}

	private User changePasswordWrapper(DbInstance dbi, Credentials credentials, String newPassword, long tryUntil) throws PasswordManagementException {
		PasswordManagementException failed = null;
		User user = new User(credentials);
		try {
			user = changePasswordInternal(dbi, credentials, newPassword, tryUntil);
		} catch (PasswordManagementException e) {
			failed = e;
		}
		log(dbi, user, Dbregistry.INTERNALDB_NAME, renewKey(), 0, (failed != null ? failed.reason : Reason.SUCCESS));
		if (failed != null)
			throw failed;
		return user;
	}

	private static final String SQL_RETRIEVE_MDALG = "SELECT value " +
												   	   "FROM parameters " +
												   	  "WHERE name = ?";
	private static final String SQL_UPDATE_PASSWORD = "UPDATE users " +
													 	 "SET password = ?, " +
													 	 	 "mdalg = ?, " +
													 	 	 "pwdexpired = ? " +
													   "WHERE username = ?";
	private User changePasswordInternal(DbInstance dbi, Credentials credentials, String newPassword, long tryUntil) throws PasswordManagementException {
		boolean reauthenticate = false;
		User user = new User(credentials);
		try {
			user = authenticateInternal(dbi, credentials, tryUntil);
		} catch (LoginFailedException e) {
			if (e.reason != Reason.RENEW_PASSWORD)
				throw e;
			reauthenticate = true;
		}
		if (credentials.password.equals(newPassword))
			throw new PasswordRenewalException("New password must be different from the old one");
		String mdalg = null;
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		parameters.add(new DbParam(DbParam.STRING, PASSWORD_MDALG_PARAMNAME, false));
		DbResultSet result = null;
		try {
			DbStatement statement = dbi.createStatement(SQL_RETRIEVE_MDALG, parameters, false);
			if (tryUntil > 0) {
				long now = System.currentTimeMillis();
				statement.setTimeout(tryUntil - now);
			}
			result = statement.execute();
			boolean algoExists = false;
			while (result.next()) {
				algoExists = true;
				mdalg = (String)result.get("value");
			}
			if (algoExists == false)
				throw new PasswordRenewalException(PASSWORD_MDALG_ERROR);
		} catch (SqlException e) {
			throw new PasswordRenewalException(e);
		} finally {
			if (result != null)
				result.close();
		}
		String storedPassword;
		if (mdalg.equals(AuthenticationPolicy.NO_DIGEST)) {
			storedPassword = newPassword;
		} else {
			try {
				MessageDigest md = MessageDigest.getInstance(mdalg);
				storedPassword = JUtility.toHexString(md.digest(newPassword.getBytes()));
			} catch (NoSuchAlgorithmException e) {
				throw new PasswordRenewalException(e);
			}
		}
		parameters.clear();
		parameters.add(new DbParam(DbParam.STRING, storedPassword, false));
		parameters.add(new DbParam(DbParam.STRING, mdalg, false));
		parameters.add(new DbParam(DbParam.BOOLEAN, "false", false));
		parameters.add(new DbParam(DbParam.STRING, credentials.username, false));
		result = null;
		try {
			DbStatement statement = dbi.createStatement(SQL_UPDATE_PASSWORD, parameters, false);
			if (tryUntil > 0) {
				long now = System.currentTimeMillis();
				statement.setTimeout(tryUntil - now);
			}
			result = statement.execute();
			dbi.commit();
		} catch (SqlException e) {
			throw new PasswordRenewalException(e);
		} finally {
			if (result != null)
				result.close();
		}
		if (reauthenticate) {
			// Create a copy of original credentials with the new password.
			Credentials newCredentials = new Credentials(credentials, newPassword);
			user = authenticateInternal(dbi, newCredentials, 0); // Must not timeout!
		}
		return user;
	}

	private static final String SQL_INSERT_LOG = "INSERT INTO authhistory(username, " +
																	 	 "dbname, " +
																	 	 "type, " +
																	 	 "result, " +
																	 	 "timestamp, " +
																	 	 "ipaddress) " +
																  "VALUES(?, ?, ?, ?, ?, ?)";
	private void log(DbInstance dbi, User user, String dbname, String type, long tryUntil, Reason result) {
		try {
			ArrayList<DbParam> parameters = new ArrayList<DbParam>();
			String username = (user.credentials.username.length() == 0 ? "(blank)" : user.credentials.username);
			parameters.add(new DbParam(DbParam.STRING, username, false));
			parameters.add(new DbParam(DbParam.STRING, dbname, false));
			parameters.add(new DbParam(DbParam.STRING, type, false));
			parameters.add(new DbParam(DbParam.INTEGER, Integer.toString(result.ordinal()), false));
			parameters.add(new DbParam(DbParam.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toString(), false));
			parameters.add(new DbParam(DbParam.STRING, user.credentials.getIpAddress(), false));
			DbStatement statement = dbi.createStatement(SQL_INSERT_LOG, parameters, false);
			if (tryUntil > 0) {
				long now = System.currentTimeMillis();
				statement.setTimeout(tryUntil - now);
			}
			statement.execute().close();
			dbi.commit();
		} catch (SqlException e) {
			e.log(); // logged and skipped.
		}
	}
}
