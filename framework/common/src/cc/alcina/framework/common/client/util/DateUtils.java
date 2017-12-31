package cc.alcina.framework.common.client.util;

import java.util.Date;

import com.google.gwt.user.datepicker.client.CalendarUtil;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Interchangable because this (gwt-compat) version is weak compared to real,
 * calendar based implementations
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = DateUtils.class, implementationType = ImplementationType.SINGLETON)
@SuppressWarnings("deprecation")
@ClientInstantiable
public class DateUtils implements RegistrableService {
	private static DateUtils singleton;

	public static int ageInDays(Date date) {
		return get().ageInDays0(date);
	}

	public static DateUtils get() {
		if (singleton == null) {
			singleton = Registry.impl(DateUtils.class);
		}
		return singleton;
	}

	@Override
	public void appShutdown() {
		singleton = null;
	}

	public DatePair getYearRange(int startingMonth, int yearOffset) {
		DatePair result = new DatePair();
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
		CommonUtils.roundDate(d, false);
	}

	protected int ageInDays0(Date date) {
		return (int) (date == null ? 0
				: (System.currentTimeMillis() - date.getTime())
						/ TimeConstants.ONE_DAY_MS);
	}
}
