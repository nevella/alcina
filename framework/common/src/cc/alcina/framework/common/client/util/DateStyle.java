package cc.alcina.framework.common.client.util;

import java.util.Date;

/**
 * DM - day_month, MD - month_day
 */
public enum DateStyle {
	DM_DATE_SLASH, DM_DATE_MONTH, DM_DATE_MONTH_DAY, DM_DATE_TIME,
	DM_DATE_TIME_HUMAN, DM_DATE_TIME_MS, DM_SHORT_DAY, DM_DATE_DOT, DM_LONG_DAY,
	DM_SHORT_MONTH, DM_DATE_SLASH_MONTH, TIMESTAMP, NAMED_MONTH_DATE_TIME_HUMAN,
	NAMED_MONTH_DAY, DM_SHORT_MONTH_SLASH, DM_SHORT_MONTH_NO_DAY,
	TIMESTAMP_HUMAN, MD_DATE_SLASH, TIMESTAMP_NO_DAY, DM_DATE_MONTH_NO_PAD_DAY,
	DM_DATE_TIME_SHORT, DATESTAMP_HUMAN, DM_DATE_TIME_TZ, DATESTAMP_DASHED;

	public String format(Date date) {
		return formatDate(date, this);
	}

	public static String formatDate(Date date, DateStyle style,
			String nullMarker) {
		return formatDate(date, style, nullMarker, null);
	}

	@SuppressWarnings("deprecation")
	static String formatDate(Date date, DateStyle style, String nullMarker,
			DateTzAdjustment.DateAdjustmentModifier dateAdjustmentModifier) {
		if (date == null) {
			return nullMarker;
		}
		DateTzAdjustment dateAdjustment = DateTzAdjustment
				.getDateTzAdjustment();
		if (dateAdjustment != null
				&& dateAdjustmentModifier != DateTzAdjustment.DateAdjustmentModifier.LOCAL_TZ) {
			switch (style) {
			case DM_DATE_TIME_TZ:
				if (dateAdjustmentModifier == null) {
					String local = formatDate(date, style, nullMarker,
							DateTzAdjustment.DateAdjustmentModifier.LOCAL_TZ);
					String adjustTo = formatDate(date, style, nullMarker,
							DateTzAdjustment.DateAdjustmentModifier.ADJUST_TO_TZ);
					return Ax.format("%s\n%s", adjustTo, local);
				}
			default:
				break;
			}
			date = dateAdjustment.adjust(date, true);
		}
		switch (style) {
		case DM_DATE_SLASH:
			return CommonUtils.format("%s/%s/%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900));
		case MD_DATE_SLASH:
			return CommonUtils.format("%s/%s/%s",
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_SLASH_MONTH:
			return CommonUtils.format("%s/%s",
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_DOT:
			return CommonUtils.format("%s.%s.%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_TIME:
			return CommonUtils.format("%s/%s/%s - %s:%s:%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()));
		case DM_DATE_TIME_TZ: {
			String formatted = CommonUtils.format("%s/%s/%s - %s:%s:%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()));
			String suffix = dateAdjustment == null ? ""
					: dateAdjustment.toSuffix(dateAdjustmentModifier);
			String prefix = dateAdjustment == null ? ""
					: dateAdjustment.toPrefix(dateAdjustmentModifier);
			return prefix + formatted + suffix;
		}
		case DM_DATE_TIME_HUMAN:
			return DM_LONG_DAY.format(date) + CommonUtils.format(" at %s:%s %s",
					CommonUtils.padTwo((date.getHours() - 1) % 12 + 1),
					CommonUtils.padTwo(date.getMinutes()),
					date.getHours() < 12 ? "AM" : "PM");
		case NAMED_MONTH_DATE_TIME_HUMAN:
			return NAMED_MONTH_DAY.format(date)
					+ CommonUtils.format(" at %s:%s %s",
							CommonUtils.padTwo((date.getHours() - 1) % 12 + 1),
							CommonUtils.padTwo(date.getMinutes()),
							date.getHours() < 12 ? "AM" : "PM");
		case NAMED_MONTH_DAY:
			return CommonUtils.format("%s, %s %s %s",
					CommonUtils.DAY_NAMES[date.getDay()],
					CommonUtils.MONTH_NAMES[date.getMonth() + 1],
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_TIME_MS:
			return CommonUtils.format("%s/%s/%s - %s:%s:%s:%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()),
					CommonUtils.padThree((int) (date.getTime() % 1000)));
		case DM_DATE_MONTH:
			return CommonUtils.format("%s %s %s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.MONTH_NAMES[date.getMonth() + 1],
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_MONTH_NO_PAD_DAY:
			return CommonUtils.format("%s %s %s", date.getDate(),
					CommonUtils.MONTH_NAMES[date.getMonth() + 1],
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_MONTH_DAY:
			return CommonUtils.format("%s %s, %s",
					CommonUtils.MONTH_NAMES[date.getMonth() + 1],
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_SHORT_MONTH:
			return CommonUtils
					.format("%s %s %s", date.getDate(),
							CommonUtils.MONTH_NAMES[date.getMonth() + 1]
									.substring(0, 3),
							CommonUtils.padTwo(date.getYear() + 1900));
		case DM_SHORT_MONTH_SLASH:
			return CommonUtils
					.format("%s/%s/%s", CommonUtils.padTwo(date.getDate()),
							CommonUtils.MONTH_NAMES[date.getMonth() + 1]
									.substring(0, 3),
							CommonUtils.padTwo(date.getYear() + 1900));
		case DM_SHORT_DAY:
			return CommonUtils.format("%s - %s.%s.%s",
					CommonUtils.DAY_NAMES[date.getDay()].substring(0, 3),
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_LONG_DAY:
			return CommonUtils.format("%s, %s.%s.%s",
					CommonUtils.DAY_NAMES[date.getDay()],
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900));
		case TIMESTAMP:
			return CommonUtils.format("%s%s%s_%s%s%s_%s",
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()),
					CommonUtils.padThree((int) (date.getTime() % 1000)));
		case TIMESTAMP_HUMAN:
			return CommonUtils.format("%s.%s.%s %s:%s:%s",
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()));
		case TIMESTAMP_NO_DAY:
			return CommonUtils.format("%s:%s:%s,%s",
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()),
					CommonUtils.padThree((int) (date.getTime() % 1000)));
		case DM_SHORT_MONTH_NO_DAY:
			return CommonUtils.format(
					"%s %s", CommonUtils.MONTH_NAMES[date.getMonth() + 1]
							.substring(0, 3),
					CommonUtils.padTwo(date.getYear() + 1900));
		case DM_DATE_TIME_SHORT:
			return CommonUtils.format("%s/%s/%s - %s:%s:%s",
					CommonUtils.padTwo(date.getDate()),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getHours()),
					CommonUtils.padTwo(date.getMinutes()),
					CommonUtils.padTwo(date.getSeconds()));
		case DATESTAMP_HUMAN:
			return CommonUtils.format("%s.%s.%s",
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getDate()));
		case DATESTAMP_DASHED:
			return CommonUtils.format("%s-%s-%s",
					CommonUtils.padTwo(date.getYear() + 1900),
					CommonUtils.padTwo(date.getMonth() + 1),
					CommonUtils.padTwo(date.getDate()));
		}
		return date.toString();
	}

	static String formatDate(Date date, DateStyle style) {
		return formatDate(date, style, " ");
	}
}