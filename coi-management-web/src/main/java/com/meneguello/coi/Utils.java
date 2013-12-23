package com.meneguello.coi;

import java.sql.Timestamp;
import java.util.Date;

public abstract class Utils {

	public static Date asDate(java.sql.Date date) {
		if (date == null) return null;
		return new Date(date.getTime());
	}
	
	public static java.sql.Date asSQLDate(Date date) {
		if (date == null) return null;
		return new java.sql.Date(date.getTime());
	}
	
	public static Timestamp asTimestamp(Date timestamp) {
		if (timestamp == null) return null;
		return new Timestamp(timestamp.getTime());
	}
	
}
