package org.jsnap.http.pages;

import java.util.ArrayList;

import org.jsnap.db.base.DbInstance;
import org.jsnap.db.base.DbParam;
import org.jsnap.db.base.DbResultSet;
import org.jsnap.db.base.DbStatement;
import org.jsnap.exception.db.SqlException;
import org.jsnap.http.base.HttpRequest;
import org.jsnap.util.JDocument;
import org.jsnap.util.JUtility;

public class AuthenticationStats extends AbstractWebMultiPage {
	public static final AuthenticationStats INSTANCE = new AuthenticationStats();

	public String logKey() {
		return "auth";
	}

	public String key() {
		String k = "stats-auth";
		if (all == false)
			k += "-" + owner;
		return k;
	}

	public String name() {
		String n = "Authentication Statistics";
		if (all == false)
			n += " for " + owner;
		else
			n += " for all databases";
		return n;
	}

	public String category() {
		return CATEGORY_STATISTICS;
	}

	public String stylesheet() {
		return "/jwc/authstats.xsl";
	}

	public boolean administrative() {
		return true;
	}

	private static final String PARAMETER_REFRESH = "refresh";
	private static final String PARAMETER_GROUPBY = "groupby";
	private static final String PARAMETER_SUCCESSFUL = "successful";
	private static final String PARAMETER_FAILED = "failed";
	private static final String GROUPBY_USERNAME = "username";
	private static final String GROUPBY_DATABASE = "dbname";
	private static final String GROUPBY_IPADDRESS = "ipaddress";
	private static final String GROUPBY_RESULT = "result";
	private static final String GROUPBY_MINUTE = "minute";
	private static final String GROUPBY_HOUR = "hour";
	private static final String GROUPBY_DAY = "day";
	private static final String GROUPBY_MONTH = "month";
	private static final String GROUPBY_YEAR = "year";
	private static final String DOCUMENT_ROOT = "auth";
	private static final String RECORD_ROOT = "group";
	private static final int MAXIMUM_FILTERS = 6;

	private Filter[] filters = new Filter[MAXIMUM_FILTERS]; 
	private boolean successful, failed;
	private String groupby;
	private final String owner;
	private final boolean all;

	public AuthenticationStats() {
		super(DOCUMENT_ROOT, RECORD_ROOT);
		this.owner = null;
		this.all = true;
	}

	public AuthenticationStats(String owner) {
		super(DOCUMENT_ROOT, RECORD_ROOT);
		this.owner = owner;
		this.all = false;
	}

	protected WebResponse runOnce(HttpRequest request) {
		return null; // No operation.
	}

	protected void initialize() {
		// No operation.
	}

	protected void appendPageData(JDocument doc) {
		doc.appendTextNode(PARAMETER_GROUPBY, groupby);
		if (successful)
			doc.appendEmptyNode(PARAMETER_SUCCESSFUL);
		if (failed)
			doc.appendEmptyNode(PARAMETER_FAILED);
		int i = 0;
		while (i < MAXIMUM_FILTERS) {
			if (filters[i] != null)
				doc.appendNodeHierarchy(Filter.FILTER_PREFIX, Filter.names, filters[i].values());
			else
				doc.appendEmptyNode(Filter.FILTER_PREFIX);
			++i;
		}
	}

	protected boolean needsRefresh(HttpRequest request) {
		return (request.parameters.get(PARAMETER_REFRESH) != null);
	}

	protected void refresh(HttpRequest request) {
		groupby = JUtility.valueOf(request.parameters.get(PARAMETER_GROUPBY), (all ? GROUPBY_DATABASE : GROUPBY_USERNAME));
		successful = (request.parameters.get(PARAMETER_SUCCESSFUL) != null);
		failed = (request.parameters.get(PARAMETER_FAILED) != null);
		if (successful == false && failed == false) {
			successful = true;
			failed = true;
		}
		int filterCount = 0;
		for (int i = 1; i <= MAXIMUM_FILTERS; ++i) {
			String filterState = Filter.FILTER_PREFIX + Integer.toString(i) + Filter.STATE;
			String state = request.parameters.get(filterState);
			if (state != null && state.equals(Filter.ON)) {
				String field = request.parameters.get(Filter.FIELD_PREFIX + Integer.toString(i));
				String operator = request.parameters.get(Filter.OPERATOR_PREFIX + Integer.toString(i));
				String value = request.parameters.get(Filter.VALUE_PREFIX + Integer.toString(i)).trim();
				if (field != null && field.equals(Filter.NONE) == false &&
					operator != null && operator.equals(Filter.NONE) == false &&
					value != null && value.length() > 0) {
					filters[filterCount] = new Filter(field, operator, value);
					++filterCount;
				}
			}
		}
		while (filterCount < MAXIMUM_FILTERS) {
			filters[filterCount] = null;
			++filterCount;
		}
	}

	protected boolean operation(JDocument doc, HttpRequest request) {
		return false; // No operation.
	}

	protected DbResultSet create(DbInstance dbi) throws SqlException {
		String sql = "SELECT ";
		String whereClause, groupbyClause;
		if (groupby.equals(GROUPBY_MINUTE) ||
			groupby.equals(GROUPBY_HOUR) ||
			groupby.equals(GROUPBY_DAY) ||
			groupby.equals(GROUPBY_MONTH) ||
			groupby.equals(GROUPBY_YEAR)) {
			groupbyClause = "TIMESTAMP_TO_STRING(timestamp, '" + groupby + "')";
		} else if (groupby.equals(GROUPBY_RESULT)) {
			groupbyClause = "REASON_TO_STRING(result)";
		} else if (groupby.equals(GROUPBY_USERNAME) || groupby.equals(GROUPBY_DATABASE) || groupby.equals(GROUPBY_IPADDRESS)) {
			groupbyClause = groupby;
		} else {
			groupbyClause = (all ? GROUPBY_DATABASE : GROUPBY_USERNAME);
		}
		ArrayList<DbParam> parameters = new ArrayList<DbParam>();
		if (groupby.equals(GROUPBY_USERNAME) || groupby.equals(GROUPBY_DATABASE) || groupby.equals(GROUPBY_IPADDRESS)) {
			sql += "TIMESTAMP_TO_STRING(MAX(CASE result WHEN 0 THEN timestamp ELSE NULL END), 'second') AS \"lastsucceeded\", " +
		  	   	   "TIMESTAMP_TO_STRING(MAX(CASE result WHEN 0 THEN NULL ELSE timestamp END), 'second') AS \"lastfailed\", ";
		}
		whereClause = "type = ?";
		parameters.add(new DbParam(DbParam.STRING, logKey(), false));
		if (all == false) {
			whereClause += " AND dbname = ?";
			parameters.add(new DbParam(DbParam.STRING, owner, false));
		}
		if (successful && failed == false) {
			whereClause += " AND result = ?";
			parameters.add(new DbParam(DbParam.INTEGER, "0", false));
		} else if (successful == false && failed) {
			whereClause += " AND result <> ?";
			parameters.add(new DbParam(DbParam.INTEGER, "0", false));
		}
		for (Filter filter: filters) {
			if (filter != null) {
				whereClause += " AND ";
				String value = filter.value;
				if (filter.field.equals(Filter.FIELD_TIMESTAMP)) {
					whereClause += "TIMESTAMP_TO_SECONDS(" + Filter.FIELD_TIMESTAMP + ")";
					if (filter.operator.equals(Filter.OPERATOR_EQUALS))
						whereClause += " = ";
					else if (filter.operator.equals(Filter.OPERATOR_GREATER))
						whereClause += " > ";
					else if (filter.operator.equals(Filter.OPERATOR_LESS))
						whereClause += " < ";
					whereClause += "STRING_TO_SECONDS(?)";
				} else {
					whereClause += filter.field;
					if (filter.operator.equals(Filter.OPERATOR_EQUALS)) {
						whereClause += " LIKE ?";
						value = value.replace('*', '%');
					} else if (filter.operator.equals(Filter.OPERATOR_GREATER)) {
						whereClause += " > ?";
					} else if (filter.operator.equals(Filter.OPERATOR_LESS)) {
						whereClause += " < ?";
					}
				}
				parameters.add(new DbParam(DbParam.STRING, value, false));
			}
		}
		sql += groupbyClause + " AS \"title\", COUNT(*) AS \"counter\" FROM authhistory WHERE " + whereClause + " GROUP BY " + groupbyClause + " ORDER BY \"counter\" DESC";
		DbStatement statement = dbi.createStatement(sql, parameters, true);
		DbResultSet result = statement.execute();
		return result;
	}

	protected static class Filter {
		public static final String STATE = "state";
		public static final String NONE = "none";
		public static final String ON = "on";
		public static final String OFF = "off";

		public static final String FILTER_PREFIX = "filter";
		public static final String FIELD_PREFIX = "field";
		public static final String OPERATOR_PREFIX = "operator";
		public static final String VALUE_PREFIX = "value";

		public static final String FIELD_USERNAME = "username";
		public static final String FIELD_DATABASE = "dbname";
		public static final String FIELD_IPADDRESS = "ipaddress";
		public static final String FIELD_TIMESTAMP = "timestamp";

		public static final String OPERATOR_EQUALS = "equal";
		public static final String OPERATOR_GREATER = "greater";
		public static final String OPERATOR_LESS = "less";

		public static final String[] names = new String[]{FIELD_PREFIX, OPERATOR_PREFIX, VALUE_PREFIX};

		public final String field;
		public final String operator;
		public final String value;

		public Filter(String field, String operator, String value) {
			this.field = field;
			this.operator = operator;
			this.value = value;
		}

		public String[] values() {
			return new String[]{field, operator, value};
		}
	}
}
