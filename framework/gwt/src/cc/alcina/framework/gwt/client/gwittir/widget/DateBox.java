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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DateBox.Format;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * This class is a bit hacked, to support Gwittir validation. It requires an
 * outgoing ShortDate validator
 * 
 * If the string date field is invalid (determined by gwt datebox validation),
 * instances emit an (invalid) String (via property change listeners) to trigger
 * Gwittir validation feedback, otherwise they emit a date. If there's no
 * validator to catch the String, you'd probably get a classcast somewhere - the
 * validator is automatically added by gwittir bridge but must be added manually
 * if using in imperative code
 * 
 * 
 * @author Nick Reddel
 */
public class DateBox extends AbstractBoundWidget<Date>
		implements ValueChangeHandler {
	private com.google.gwt.user.datepicker.client.DateBox base;;

	private DateTimeFormat unAmerican = DateTimeFormat.getFormat("dd/MM/yyyy");

	private String text;

	private Date value;

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

	public Date getValue() {
		return base.getValue();
	}

	public void onValueChange(ValueChangeEvent event) {
		fireChangesFromBase();
	}

	public void setValue(Date value) {
		Date oldDate = this.value;
		this.value = value;
		base.setValue(value, false);
		changes.firePropertyChange("value", oldDate, this.value);
	}

	private void fireChangesFromBase() {
		String oldText = this.text;
		this.text = base.getTextBox().getText();
		if (base.getStyleName().contains("dateBoxFormatError")) {
			changes.firePropertyChange("value", oldText, this.text);
			return;
		}
		setValue(base.getValue());
	}

	protected DateTimeFormat getDateTimeFormat() {
		return unAmerican;
	}

	public static class DateBoxProvider
			implements BoundWidgetProvider<DateBox> {
		public DateBox get() {
			return new DateBox();
		}
	}
}
