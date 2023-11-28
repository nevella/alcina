package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractModelTransform;

public class LeafTransforms {
	public static class DateDot extends AbstractModelTransform<Date, String> {
		@Override
		public String apply(Date t) {
			return CommonUtils.formatDate(t, DateStyle.AU_DATE_DOT);
		}
	}
}
