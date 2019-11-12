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
package cc.alcina.framework.gwt.client.gwittir.customiser;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.BidiConverter;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.widget.DateBox;
import cc.alcina.framework.gwt.client.gwittir.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.util.ClientUtils;

@ClientInstantiable
/**
 *
 * @author Nick Reddel
 */
public class DateBoxCustomiser implements Customiser, BoundWidgetProvider {
	public static final String UTC = "UTC";

	public static final String ISO_8601 = "ISO_8601";

	private boolean utc;

	private boolean iso8601;

	private boolean editable;

	public static class UtcDateRenderer implements Renderer<Date, String> {
		@Override
		public String render(Date date) {
			return date == null ? "" : render0(date.getTime());
		}

		public static final native String render0(double millis) /*-{
      var jsDate = new Date(millis);
      return jsDate.toUTCString();
		}-*/;
	}
@ClientInstantiable
	public static class ISO_8601_DateRenderer
			implements Renderer<Date, String> {
		@Override
		public String render(Date date) {
			return date == null ? ""
					: DateTimeFormat.getFormat(PredefinedFormat.ISO_8601)
							.format(date);
		}
	}

	public static class UtcLocalDateTranslator
			extends BidiConverter<Date, Date> {
		@Override
		public Date leftToRight(Date a) {
			if (a == null) {
				return null;
			}
			int tzOffsetMinutes = ClientUtils.getDateTzOffsetMinutes();
			return new Date(a.getTime() + tzOffsetMinutes * 60 * 1000);
		}

		@Override
		public Date rightToLeft(Date b) {
			if (b == null) {
				return null;
			}
			int tzOffsetMinutes = ClientUtils.getDateTzOffsetMinutes();
			return new Date(b.getTime() - tzOffsetMinutes * 60 * 1000);
		}
	}

	@Override
	public BoundWidget get() {
		if (utc) {
			if (editable) {
				DateBox dateBox = new DateBox();
				dateBox.setDateTranslator(new UtcLocalDateTranslator());
				return dateBox;
			} else {
				RenderingLabel<Date> label = new RenderingLabel<Date>();
				label.setRenderer(new UtcDateRenderer());
				return label;
			}
		} else if (iso8601) {
			if (editable) {
				DateBox dateBox = new DateBox(
						DateTimeFormat.getFormat(PredefinedFormat.ISO_8601));
				dateBox.addStyleName("iso8601");
				return dateBox;
			} else {
				RenderingLabel<Date> label = new RenderingLabel<Date>();
				label.setRenderer(new ISO_8601_DateRenderer());
				return label;
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom info) {
		this.editable = editable;
		utc = NamedParameter.Support.booleanValue(info.parameters(), UTC);
		iso8601 = NamedParameter.Support.booleanValue(info.parameters(),
				ISO_8601);
		return this;
	}
}
