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
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DateBox.Format;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * This class is a bit hacked, to support Gwittir validation (if we just had it
 * as an ABW<Date>, validation errors would never get to the Gwittir validation
 * s/s). It requires an incoming DateToLongString converter, and an outgoing ShortDate validator 
 * 
 * @author Nick Reddel
 */
public class DateBox extends AbstractBoundWidget<String> implements
		ValueChangeHandler {
	public static final BoundWidgetProvider PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			return new DateBox();
		}
	};

	private com.google.gwt.user.datepicker.client.DateBox base;

	private DateTimeFormat unAmerican = DateTimeFormat.getFormat("dd/MM/yyyy");

	private String oldText;

	protected DateTimeFormat getDateTimeFormat() {
		return unAmerican;
	}

	public DateBox() {
		Format dtFormat = new DefaultFormat(getDateTimeFormat());
		DatePicker picker = new DatePicker();
		picker.addStyleName("alcina-DatePicker");
		base = new com.google.gwt.user.datepicker.client.DateBox(picker, null,
				dtFormat);
		base.getTextBox().addValueChangeHandler(this);
		base.addValueChangeHandler(this);
		base.addStyleName("alcina-DateBox");
		initWidget(base);
	}

	public String getValue() {
		return base.getTextBox().getValue();
	}

	public Date getValueAsDate() {
		return base.getValue();
	}

	public void setValue(Date value) {
		String old = getValue();
		base.setValue(value);
		oldText = getValue();
		changes.firePropertyChange("value", old, this.getValue());
	}

	private void fireChangesFromBase() {
		changes.firePropertyChange("value", oldText, getValue());
		oldText = getValue();
	}

	public void onValueChange(ValueChangeEvent event) {
		fireChangesFromBase();
	}

	/**
	 * Should always be setting a stringified long
	 */
	public void setValue(String value) {
		if (value != null) {
			try {
				long l = Long.parseLong(value);
				setValue(new Date(l));
			} catch (Exception e) {
				GWT.log("Setting illegal datebox string value - " + value);
			}
		}
	}
}
