package cc.alcina.framework.gwt.client.util;

import java.util.Date;

import cc.alcina.framework.common.client.util.TimezoneData;

@SuppressWarnings("deprecation")
public class TimezoneDataProviderClient implements TimezoneData.Provider {
	static native String getTimeZone()/*-{
    try {
      return Intl.DateTimeFormat().resolvedOptions().timeZone;
    } catch (e) {
      return "(Unknown timezone)";
    }
	}-*/;

	@Override
	public TimezoneData getTimezoneData() {
		TimezoneData localData = new TimezoneData();
		localData.setTimeZone(getTimeZone());
		localData.setUtcMinutes(new Date().getTimezoneOffset());
		return localData;
	}
}
