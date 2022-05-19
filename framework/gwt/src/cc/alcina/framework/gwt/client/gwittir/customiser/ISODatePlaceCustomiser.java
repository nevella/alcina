package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

public class ISODatePlaceCustomiser extends ModelPlaceCustomiser {
	@Override
	public ModelPlaceRenderer provideRenderer() {
		return new ISODateRenderer();
	}

	private static class ISODateRenderer extends ModelPlaceRenderer {
		@Override
		public String render(Object value) {
			if (value instanceof Date) {
				value = DateTimeFormat.getFormat(PredefinedFormat.ISO_8601)
						.format((Date) value);
			}
			return super.render(value);
		}
	}
}
