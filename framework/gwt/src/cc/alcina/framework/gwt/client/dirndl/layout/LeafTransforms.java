package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;

import cc.alcina.framework.common.client.util.DateStyle;

public class LeafTransforms {
	public interface Dates {
		public static class Dot implements ModelTransform<Date, String> {
			@Override
			public String apply(Date t) {
				return DateStyle.DATE_DOT.format(t);
			}
		}

		public static class TimestampHuman
				implements ModelTransform<Date, String> {
			@Override
			public String apply(Date t) {
				return DateStyle.TIMESTAMP_HUMAN.format(t);
			}
		}

		public static class TimestampNoDay
				implements ModelTransform<Date, String> {
			@Override
			public String apply(Date t) {
				return DateStyle.TIMESTAMP_NO_DAY.format(t);
			}
		}
	}
}
