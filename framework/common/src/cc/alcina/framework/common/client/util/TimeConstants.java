/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.util;

import java.util.Date;

/**
 *
 * @author Nick Reddel
 */
public class TimeConstants {
	public static final long ONE_SECOND_MS = 1000;

	public static final long ONE_MINUTE_MS = ONE_SECOND_MS * 60;

	public static final long ONE_HOUR_MS = ONE_MINUTE_MS * 60;

	public static final long ONE_DAY_MS = ONE_HOUR_MS * 24;

	public static final long ONE_WEEK_MS = ONE_DAY_MS * 7;

	public static final long ONE_YEAR_MS = ONE_DAY_MS * 365;

	public static Date nowMinusDays(int days) {
		return new Date(System.currentTimeMillis() - days * ONE_DAY_MS);
	}

	public static String toDurationString(long millis) {
		if (millis < ONE_SECOND_MS) {
			return Ax.format("%sms", millis);
		}
		if (millis < ONE_HOUR_MS) {
			return Ax.format("%ss",
					Ax.fourPlaces(((double) millis) / ONE_SECOND_MS));
		}
		if (millis < ONE_DAY_MS) {
			return Ax.format("%sh",
					Ax.fourPlaces(((double) millis) / ONE_HOUR_MS));
		}
		return Ax.format("%sd", Ax.fourPlaces(((double) millis) / ONE_DAY_MS));
	}

	public static boolean within(long time, long delta) {
		return (System.currentTimeMillis() - time) < delta;
	}

	public static boolean withinDays(Date date, int days) {
		return date == null ? false : within(date.getTime(), days * ONE_DAY_MS);
	}
}
