package cc.alcina.framework.gwt.client.impl;

import java.util.Date;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

public class FlatTreeSerializer_ExtensionImpl
		implements FlatTreeSerializer.Extension {
	@Override
	public boolean isValueType(Class clazz) {
		if (Reflections.isAssignableFrom(BasePlace.class, clazz)) {
			return true;
		}
		return false;
	}

	@Override
	public Object synthesiseSimpleValue(Class valueClass) {
		if (Reflections.isAssignableFrom(BasePlace.class, valueClass)) {
			BasePlace place = (BasePlace) Reflections.newInstance(valueClass);
			// deliberately non-persistent so that testing roundtrip is stable
			if (place instanceof EntityPlace) {
				((EntityPlace) place).id = -1;
			}
			return place;
		}
		return null;
	}

	@Override
	public Object parseStringValue(Class valueClass, String stringValue) {
		if (Reflections.isAssignableFrom(BasePlace.class, valueClass)) {
			return RegistryHistoryMapper.get().getPlaceOrThrow(stringValue);
		}
		return null;
	}

	@Override
	public String toStringValue0(Object value) {
		if (value instanceof BasePlace) {
			return ((BasePlace) value).toTokenString();
		}
		return null;
	}

	static final RegExp TIME_FORMAT = RegExp.compile(
			"(\\d{4})(\\d{2})(\\d{2})T(\\d{2})(\\d{2})(\\d{2})(\\d{3})([+\\-])(\\d{2})(\\d{2})");

	@Override
	/*
	 * yes yes, date deprecation, yes yes
	 */
	@SuppressWarnings("deprecation")
	public long parseTime(String stringValue) {
		MatchResult result = TIME_FORMAT.exec(stringValue);
		int year = Integer.parseInt(result.getGroup(1)) - 1900;
		int month = Integer.parseInt(result.getGroup(2)) - 1;
		int day = Integer.parseInt(result.getGroup(3));
		int hrs = Integer.parseInt(result.getGroup(4));
		int min = Integer.parseInt(result.getGroup(5));
		int sec = Integer.parseInt(result.getGroup(6));
		int millis = Integer.parseInt(result.getGroup(7));
		String plusMinus = result.getGroup(8);
		int tzHours = Integer.parseInt(result.getGroup(9));
		int tzMinutes = Integer.parseInt(result.getGroup(10));
		int tzTotalMinutes = tzHours * 60 + tzMinutes;
		int tzMultiplier = plusMinus.equals("+") ? 1 : -1;
		Date date = new Date(year, month, day, hrs, min, sec);
		long time = date.getTime();
		time += millis;
		int tzOffset = date.getTimezoneOffset();
		// adjust for tz diff
		time += date.getTimezoneOffset() * TimeConstants.ONE_MINUTE_MS;
		time += tzMultiplier * tzTotalMinutes * TimeConstants.ONE_MINUTE_MS;
		return time;
	}
}