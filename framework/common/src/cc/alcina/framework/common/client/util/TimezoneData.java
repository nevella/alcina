package cc.alcina.framework.common.client.util;

import cc.alcina.framework.common.client.serializer.TreeSerializable;

public class TimezoneData implements TreeSerializable {
	private String timeZone;

	private int utcMinutes;

	public TimezoneData() {
	}

	public String getTimeZone() {
		return timeZone;
	}

	public int getUtcMinutes() {
		return utcMinutes;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public void setUtcMinutes(int utcMinutes) {
		this.utcMinutes = utcMinutes;
	}

	@Override
	public String toString() {
		double hours = -(utcMinutes / 60);
		return Ax.format("%s - UTC%s%s", timeZone, hours > 0 ? "+" : "",
				Ax.twoPlaces(hours));
	}

	public interface Provider {
		TimezoneData getTimezoneData();
	}
}