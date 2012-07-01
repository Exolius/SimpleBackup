package com.exolius.simplebackup;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateModification {

	private enum dates {
		h, d, w, m, y;
	}

	private int field;
	int amount;

	public DateModification(int field, int amount) {
		this.field = field;
		this.amount = amount;
	}

	public void moveForward(Calendar cal) {
		if (amount == 0) {
			throw new UnsupportedOperationException();
		}
		cal.add(field, amount);
	}

	public void moveBack(Calendar cal) {
		if (amount == 0) {
			throw new UnsupportedOperationException();
		}
		cal.add(field, -amount);
	}

	public static DateModification fromString(String firstDate) {
		if ("0".equals(firstDate)) {
			return new DateModification(Calendar.DATE, 0);
		}
		Matcher matcher = Pattern.compile("(\\d+)([hdwmyHDWMY])").matcher(
				firstDate);
		if (matcher.matches()) {
			String timesCount = matcher.group(1);
			String dateString = matcher.group(2);
			int count;
			try {
				count = Integer.parseInt(timesCount);
			} catch (NumberFormatException e) {
				return null;
			}
			int unit;
			switch (dates.valueOf(dateString.toLowerCase())) {
			case h:
				unit = Calendar.HOUR;
				break;
			case d:
				unit = Calendar.DATE;
				break;
			case w:
				unit = Calendar.WEEK_OF_YEAR;
				break;
			case m:
				unit = Calendar.MONTH;
				break;
			case y:
				unit = Calendar.YEAR;
				break;
			default:
				return null;
			}
			return new DateModification(unit, count);

		}
		return null;
	}

}
