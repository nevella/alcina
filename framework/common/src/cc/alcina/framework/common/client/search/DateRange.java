package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public enum DateRange {
	CURRENT_MONTH, CURRENT_YEAR, CURRENT_FINANCIAL_YEAR, LAST_3_MONTHS,
	LAST_24_HOURS, LAST_WEEK, LAST_HOUR;
}
