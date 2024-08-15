package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;

import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractModelTransform;

public class LeafTransforms {
	public static class DateDot extends AbstractModelTransform<Date, String> {
		@Override
		public String apply(Date t) {
			return DateStyle.DATE_DOT.format(t);
		}
	}

	public static class TimestampHuman
			extends AbstractModelTransform<Date, String> {
		@Override
		public String apply(Date t) {
			return DateStyle.TIMESTAMP_HUMAN.format(t);
		}
	}
}
