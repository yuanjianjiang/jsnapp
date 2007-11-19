package org.jsnap.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

public final class JUtility {
	public static int intValueOf(Object o, int error) {
		try {
			if (o instanceof Long)
				return new Long((Long)o).intValue();
			else if (o instanceof Integer)
				return (Integer)o;
			else
				return Integer.valueOf(o.toString());
		} catch (NumberFormatException ignore) {
			Logger.getLogger(JUtility.class).debug(ignore);
			return error;
		}
	}

	public static int valueOf(String value, int error) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException ignore) {
			Logger.getLogger(JUtility.class).debug(ignore);
			return error;
		}
	}

	public static long valueOf(String value, long error) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException ignore) {
			Logger.getLogger(JUtility.class).debug(ignore);
			return error;
		}
	}

	public static double valueOf(String value, double error) {
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException ignore) {
			Logger.getLogger(JUtility.class).debug(ignore);
			return error;
		}
	}

	public static String valueOf(String value, String defaultValue) {
		return (value == null ? defaultValue : value);
	}

	private static final String TIMESTAMP_FORMAT = "dd/MM/yyyy HH:mm:ss";
	private static final String MINUTE_FORMAT = "dd/MM/yyyy HH:mm";
	private static final String HOUR_FORMAT = "dd/MM/yyyy HH";
	private static final String DAY_FORMAT = "dd/MM/yyyy";
	private static final String MONTH_FORMAT = "MM/yyyy";
	private static final String YEAR_FORMAT = "yyyy";

	public static Date valueOf(String s) {
		SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
		try {
			return sdf.parse(s);
		} catch (ParseException ignore) {
			Logger.getLogger(JUtility.class).debug(ignore);
			return null;
		}
	}

	public static long sToSeconds(String timestamp) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
		return sdf.parse(timestamp).getTime() / 1000;
	}

	public static long tsToSeconds(Timestamp t) {
		if (t == null)
			return 0;
		return t.getTime() / 1000;
	}

	public static String toString(Timestamp t, String precision) {
		if (t == null)
			return null;
		SimpleDateFormat sdf;
		if (precision.equals("minute"))
			sdf = new SimpleDateFormat(MINUTE_FORMAT);
		else if (precision.equals("hour"))
			sdf = new SimpleDateFormat(HOUR_FORMAT);
		else if (precision.equals("day"))
			sdf = new SimpleDateFormat(DAY_FORMAT);
		else if (precision.equals("month"))
			sdf = new SimpleDateFormat(MONTH_FORMAT);
		else if (precision.equals("year"))
			sdf = new SimpleDateFormat(YEAR_FORMAT);
		else
			sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
		return sdf.format(new Date(t.getTime()));
	}

	public static String toString(long millis) {
		SimpleDateFormat sdf = new SimpleDateFormat(TIMESTAMP_FORMAT);
		return sdf.format(new Date(millis));
	}

	public static String toHexString(byte[] b) {
		int n;
		String result = "";
		for (byte x : b) {
			n = (x & 0xF0) >> 4;
			result += nibbleToChar(n);
			n = x & 0x0F;
			result += nibbleToChar(n);
		}
		return result;
	}

	private static char nibbleToChar(int n) {
		switch (n & 0x0F) {
		case 1: return '1';
		case 2: return '2';
		case 3: return '3';
		case 4: return '4';
		case 5: return '5';
		case 6: return '6';
		case 7: return '7';
		case 8: return '8';
		case 9: return '9';
		case 10: return 'A';
		case 11: return 'B';
		case 12: return 'C';
		case 13: return 'D';
		case 14: return 'E';
		case 15: return 'F';
		default: return '0';
		}
	}
}
