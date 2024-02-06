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
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DateBox.Format;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.collections.BidiConverter;
import cc.alcina.framework.common.client.collections.BidiConverter.BidiIdentityConverter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateAdjustment;

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
 * FIXME - dirndl 1x2 - the structure of this is sort of wrong - it'd be better
 * to have a textbox, converter, and popdown date selector (i.e. getValue()
 * returns text, not a date))
 *
 * ...and Dirndlise. That'll fix issues like "form validation works" for invalid
 * strings
 *
 * @author Nick Reddel
 */
public class DateBox extends AbstractBoundWidget<Date>
		implements ValueChangeHandler, Focusable {
	private com.google.gwt.user.datepicker.client.DateBox base;

	private DateTimeFormat dateTimeFormat;

	private String text;

	private Date value;

	private BidiConverter<Date, Date> dateTranslator = new BidiIdentityConverter<Date>();

	private DatePicker picker;

	public DateBox() {
		this(DateTimeFormat.getFormat("dd/MM/yyyy"));
	}

	public DateBox(DateTimeFormat dateTimeFormat) {
		this.dateTimeFormat = dateTimeFormat;
		Format dtFormat = new DefaultFormat(getDateTimeFormat());
		picker = new DatePicker();
		picker.addStyleName("alcina-DatePicker");
		base = createDateBox(dtFormat, picker);
		base.getTextBox().addValueChangeHandler(this);
		base.getTextBox().getElement().setAttribute("autocomplete", "off");
		base.addValueChangeHandler(this);
		base.addStyleName("alcina-DateBox");
		initWidget(base);
	}

	protected com.google.gwt.user.datepicker.client.DateBox
			createDateBox(Format dtFormat, DatePicker picker) {
		return new com.google.gwt.user.datepicker.client.DateBox(picker, null,
				dtFormat);
	}

	private void fireChangesFromBase() {
		String oldText = this.text;
		this.text = base.getTextBox().getText();
		if (base.getStyleName().contains("dateBoxFormatError")) {
			changes.firePropertyChange("value", oldText, this.text);
			return;
		}
		setValue(dateTranslator.rightToLeft(base.getValue()));
	}

	protected DateTimeFormat getDateTimeFormat() {
		return dateTimeFormat;
	}

	public BidiConverter<Date, Date> getDateTranslator() {
		return this.dateTranslator;
	}

	public DatePicker getPicker() {
		return this.picker;
	}

	@Override
	public int getTabIndex() {
		return getTextBox().getTabIndex();
	}

	private TextBox getTextBox() {
		return base.getTextBox();
	}

	@Override
	public Date getValue() {
		return dateTranslator.rightToLeft(base.getValue());
	}

	@Override
	public void onValueChange(ValueChangeEvent event) {
		fireChangesFromBase();
	}

	@Override
	public void setAccessKey(char key) {
		getTextBox().setAccessKey(key);
	}

	public void setDateTranslator(BidiConverter<Date, Date> dateTranslator) {
		this.dateTranslator = dateTranslator;
	}

	@Override
	public void setFocus(boolean focused) {
		getTextBox().setFocus(focused);
	}

	@Override
	public void setTabIndex(int index) {
		getTextBox().setTabIndex(index);
	}

	@Override
	public void setValue(Date value) {
		Date oldDate = this.value;
		this.value = value;
		base.setValue(dateTranslator.leftToRight(value), false);
		changes.firePropertyChange("value", oldDate, this.value);
	}

	public static class DateBoxProvider implements BoundWidgetProvider {
		@Override
		public DateBox get() {
			DateBox dateBox = new DateBox();
			DateAdjustment dateAdjustment = CommonUtils.getDateAdjustment();
			if (dateAdjustment != null) {
				dateBox.setDateTranslator(dateAdjustment);
			}
			return dateBox;
		}
	}
}
