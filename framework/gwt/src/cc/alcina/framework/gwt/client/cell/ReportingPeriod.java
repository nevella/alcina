package cc.alcina.framework.gwt.client.cell;

import java.util.Date;

import com.google.gwt.user.datepicker.client.CalendarUtil;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DatePair;

@ClientInstantiable
public enum ReportingPeriod {
	MONTH, QUARTER, YEAR, FINANCIAL_YEAR;
	@SuppressWarnings("deprecation")
	public DatePair toDateRange(Date date) {
		Date start = new Date(date.getTime());
		Date end = null;
		start.setDate(1);
		start = CommonUtils.roundDate(start, false);
		switch (this) {
		case MONTH:
			end = new Date(start.getTime());
			CalendarUtil.addMonthsToDate(end, 1);
			break;
		case QUARTER:
			start.setMonth(start.getMonth() % 3);
			end = new Date(start.getTime());
			CalendarUtil.addMonthsToDate(end, 3);
			break;
		case YEAR:
			start.setMonth(0);
			end = new Date(start.getTime());
			CalendarUtil.addMonthsToDate(end, 12);
			break;
		case FINANCIAL_YEAR:
			if (start.getMonth() < 6) {
				start.setYear(start.getYear() - 1);
			}
			start.setMonth(6);
			end = new Date(start.getTime());
			CalendarUtil.addMonthsToDate(end, 12);
			break;
		}
		CalendarUtil.addDaysToDate(end, -1);
		return new DatePair(start, end);
	}
}
