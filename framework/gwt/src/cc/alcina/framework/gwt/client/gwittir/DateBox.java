package cc.alcina.framework.gwt.client.gwittir;

import java.util.Date;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.datepicker.client.DatePicker;
import com.google.gwt.user.datepicker.client.DateBox.DefaultFormat;
import com.google.gwt.user.datepicker.client.DateBox.Format;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

public class DateBox extends AbstractBoundWidget<Date, String> implements
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
	private void fireChangesFromBase(){
		changes.firePropertyChange("value", oldText, getValue());
		oldText = getValue();
	}
	public void onValueChange(ValueChangeEvent event) {
		fireChangesFromBase();
	}
}
