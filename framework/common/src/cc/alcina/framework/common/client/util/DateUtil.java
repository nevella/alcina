package cc.alcina.framework.common.client.util;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.datepicker.client.CalendarUtil;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Interchangable because this (gwt-compat) version is weak compared to real,
 * calendar based implementations
 *
 *
 */
@SuppressWarnings("deprecation")
public class DateUtil {
	public interface YearResolver {
		int getYear(Date d);
	}

	public interface MonthResolver {
		int getMonth(Date d);
	}

	public static int ageInDays(Date date) {
		return (int) (date == null ? 0
				: (System.currentTimeMillis() - date.getTime())
						/ TimeConstants.ONE_DAY_MS);
	}

	public static int ageInMinutes(Date date) {
		return (int) (date == null ? 0
				: (System.currentTimeMillis() - date.getTime())
						/ TimeConstants.ONE_MINUTE_MS);
	}

	public DatePair getYearRange(int startingMonth, int yearOffset) {
		DatePair result = new DatePair(new Date(), new Date());
		toMonth(result.d1, startingMonth - 1);
		if (result.d1.after(new Date())) {
			CalendarUtil.addMonthsToDate(result.d1, -12);
		}
		CalendarUtil.addMonthsToDate(result.d1, 12 * yearOffset);
		result.d2 = new Date(result.d1.getTime());
		CalendarUtil.addMonthsToDate(result.d2, 12);
		return result;
	}

	private void toMonth(Date d, int month) {
		d.setMonth(month);
		CalendarUtil.setToFirstDayOfMonth(d);
		DateUtil.roundDate(d, false);
	}

	public static boolean isInCurrentMonth(Date date) {
		if (date == null) {
			return false;
		}
		Date now = new Date();
		return now.getYear() == date.getYear()
				&& now.getMonth() == date.getMonth();
	}

	public static String toYearMonth(Date date) {
		return date == null ? null
				: CommonUtils.format("%sM%s",
						CommonUtils.padFour(1900 + date.getYear()),
						CommonUtils.padTwo(date.getMonth() + 1));
	}

	public static Date plusDays(Date date, int days) {
		// yeah, rubbish wrt leap seconds not fussed
		return new Date(date.getTime() + TimeConstants.ONE_DAY_MS * days);
	}

	public static Date yearAsDate(Integer year) {
		if (year == null) {
			year = 0;
		}
		Date d = new Date(0);
		d.setYear(year - 1900);
		d.setMonth(0);
		d.setDate(1);
		return d;
	}

	// to 00.00:00 or 23:59.59
	public static Date roundDate(Date d, boolean up) {
		d.setHours(up ? 23 : 0);
		d.setMinutes(up ? 59 : 0);
		d.setSeconds(up ? 59 : 0);
		d.setTime(d.getTime() - d.getTime() % 1000);
		return d;
	}

	public static boolean areCloseDates(Date d1, Date d2, long ms) {
		if (d1 == null || d2 == null) {
			return d1 == d2;
		}
		return Math.abs(d1.getTime() - d2.getTime()) < ms;
	}

	public static Date cloneDate(Date date) {
		return date == null ? null : new Date(date.getTime());
	}

	public static String dateStampMillis() {
		Date d = new Date();
		return CommonUtils.format("%s%s%s%s%s%s",
				CommonUtils.padFour(d.getYear() + 1900),
				CommonUtils.padTwo(d.getMonth() + 1),
				CommonUtils.padTwo(d.getDate()),
				CommonUtils.padTwo(d.getHours()),
				CommonUtils.padTwo(d.getMinutes()),
				CommonUtils.padTwo(d.getSeconds()),
				CommonUtils.padThree((int) (d.getTime() % 1000)));
	}

	public static Date oldDate(int year, int month, int dayOfMonth) {
		Date date = new Date();
		date.setYear(year - 1900);
		date.setMonth(month - 1);
		date.setDate(dayOfMonth);
		date = DateUtil.roundDate(date, false);
		return date;
	}

	public static int getYear(Date d) {
		if (GWT.isClient()) {
			return d.getYear() + 1900;
		} else {
			return Registry.impl(YearResolver.class).getYear(d);
		}
	}

	/**
	 * @param d
	 * @return the month (zero-based)
	 */
	public static int getMonth(Date d) {
		if (GWT.isClient()) {
			return d.getMonth();
		} else {
			return Registry.impl(MonthResolver.class).getMonth(d);
		}
	}

	/**
	 * 
	 * return the month, including for a short string (jul, JUL)
	 * 
	 * @param monthString
	 * @return the month (zero-based)
	 */
	public static int getMonth(String monthString) {
		String cmp = monthString.toLowerCase();
		for (int idx = 0; idx < CommonUtils.MONTH_NAMES.length; idx++) {
			String monthName = CommonUtils.MONTH_NAMES[idx].toLowerCase();
			if ((cmp.length() == 3 && monthName.startsWith(cmp))
					|| monthName.equals(cmp)) {
				return idx;
			}
		}
		return -1;
	}

	public static Date parseYyyyMmDd(String ymd) {
		RegExp regExp = RegExp.compile("(\\d{4}).(\\d{2}).(\\d{2})");
		MatchResult matchResult = regExp.exec(ymd);
		return Ax.date(Integer.parseInt(matchResult.getGroup(1)),
				Integer.parseInt(matchResult.getGroup(2)),
				Integer.parseInt(matchResult.getGroup(3)));
	}

	public static Date parseDdMmYyyy(String ymd) {
		RegExp regExp = RegExp.compile("(\\d{2}).(\\d{2}).(\\d{4})");
		MatchResult matchResult = regExp.exec(ymd);
		return Ax.date(Integer.parseInt(matchResult.getGroup(3)),
				Integer.parseInt(matchResult.getGroup(2)),
				Integer.parseInt(matchResult.getGroup(1)));
	}

	/**
	 * 
	 * @param dayMonthnameYear
	 *            e.g. 2 June 2025 or 01 Feb 1992
	 * @return
	 */
	public static Date parseDdMmmYyyy(String dayMonthnameYear) {
		RegExp regExp = RegExp.compile("(\\d{1,2}) (\\S+) (\\d{4})");
		MatchResult matchResult = regExp.exec(dayMonthnameYear);
		return Ax.date(Integer.parseInt(matchResult.getGroup(3)),
				getMonth(matchResult.getGroup(2)),
				Integer.parseInt(matchResult.getGroup(1)));
	}
}
