package cc.alcina.framework.common.client.util;

import java.util.Date;

import cc.alcina.framework.common.client.collections.BidiConverter;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * This controls how dates are *rendered* (including in editors) - not their
 * internal representation.
 * 
 * For use where the client (browser) is in a different TZ to the server, but
 * you want to represent dates in the server tz
 */
public class DateTzAdjustment implements BidiConverter<Date, Date> {
	enum DateAdjustmentModifier {
		LOCAL_TZ, ADJUST_TO_TZ
	}

	TimezoneData localData;

	TimezoneData adjustToData;

	@Registration.Singleton
	@Registration.EnvironmentSingleton
	public static class ContextAdjustment {
		public DateTzAdjustment dateTzAdjustment;

		public static ContextAdjustment get() {
			return Registry.impl(ContextAdjustment.class);
		}
	}

	public DateTzAdjustment(TimezoneData localData, TimezoneData adjustToData) {
		this.localData = localData;
		this.adjustToData = adjustToData;
	}

	public Date adjust(Date date, boolean toAdjustTz) {
		// Null dates adjust to null dates
		if (date == null) {
			return null;
		}
		return toAdjustTz
				? new Date(date.getTime()
						+ localData.getUtcMinutes()
								* TimeConstants.ONE_MINUTE_MS
						- adjustToData.getUtcMinutes()
								* TimeConstants.ONE_MINUTE_MS)
				: new Date(date.getTime()
						- localData.getUtcMinutes()
								* TimeConstants.ONE_MINUTE_MS
						+ adjustToData.getUtcMinutes()
								* TimeConstants.ONE_MINUTE_MS);
	}

	@Override
	public Date leftToRight(Date date) {
		return adjust(date, true);
	}

	@Override
	public Date rightToLeft(Date date) {
		return adjust(date, false);
	}

	public String toPrefix(
			DateTzAdjustment.DateAdjustmentModifier dateAdjustmentModifier) {
		if (dateAdjustmentModifier == null) {
			return "";
		}
		switch (dateAdjustmentModifier) {
		case LOCAL_TZ:
			return Ax.format("local: ", localData);
		case ADJUST_TO_TZ:
			return Ax.format("server: ", adjustToData);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public String toSuffix(
			DateTzAdjustment.DateAdjustmentModifier dateAdjustmentModifier) {
		if (dateAdjustmentModifier == null) {
			return "";
		}
		switch (dateAdjustmentModifier) {
		case LOCAL_TZ:
			return Ax.format(" (%s)", localData);
		case ADJUST_TO_TZ:
			return Ax.format(" (%s)", adjustToData);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public String toUiIndicatorLabel() {
		return Ax.format("TZ: %s", adjustToData.getTimeZone());
	}

	public String toUiIndicatorTitle() {
		return Ax.format("Current time :: %s",
				DateStyle.DATE_TIME_TZ.format(new Date()));
	}

	public static DateTzAdjustment getDateTzAdjustment() {
		return ContextAdjustment.get().dateTzAdjustment;
	}
}