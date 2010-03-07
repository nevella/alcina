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

package cc.alcina.framework.gwt.client.gwittir;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.TextBox;
import com.totsp.gwittir.client.ui.calendar.Calendar;
import com.totsp.gwittir.client.ui.calendar.CalendarDrawListener;
import com.totsp.gwittir.client.ui.calendar.CalendarListener;
import com.totsp.gwittir.client.ui.calendar.DatePicker;
import com.totsp.gwittir.client.ui.calendar.Renderers;
import com.totsp.gwittir.client.ui.calendar.SourcesCalendarDrawEvents;
import com.totsp.gwittir.client.ui.calendar.SourcesCalendarEvents;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * Renders a Date value to a Label with a Image src of
 * "[module root]/calendar-icon.gif", which when clicked, will pop up a
 * DatePicker for selection.
 * 
 * @author rcooper
 */
@SuppressWarnings("deprecation")
public class PopupDatePickerWithText extends AbstractBoundWidget implements
		SourcesCalendarDrawEvents, SourcesCalendarEvents, Renderers {
	DatePicker base = new DatePicker();

	TextBox textBox = new TextBox();

	Image icon = new Image(GWT.getModuleBaseURL() + "/calendar-icon.gif");


	HorizontalPanel hp = new HorizontalPanel();

	FlowPanel holder = new FlowPanel();

	PopupPanel pp = new PopupPanel(true);

	public static final Renderer SHORT_DATE_RENDERER = new Renderer() {
		public Object render(Object o) {
			Date d = (Date) o;
			return CommonUtils.formatDate(d, DateStyle.AU_DATE_SLASH);
		}
	};

	public static final BoundWidgetProvider PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			return new PopupDatePickerWithText();
		}
	};

	/** Creates a new instance of PopupDatePicker */
	public PopupDatePickerWithText() {
		hp.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
		this.setRenderer(SHORT_DATE_RENDERER);
		RelativePopupValidationFeedback pvf = new RelativePopupValidationFeedback(
				RelativePopupValidationFeedback.BOTTOM);
		pvf.setCss("dateChooser");
		Binding b = new Binding(textBox, "value",
				GwittirBridge.DATE_TEXT_VALIDATOR, pvf, base, "value", null,
				null);
		b.setLeft();
		b.bind();
		pp.setWidget(base);
		this.hp.add(this.textBox);
		this.textBox.setStyleName("calendarText");
		icon.setStyleName("calendarIcon");
		this.hp.add(icon);
		holder.add(hp);
		holder.setStyleName("gwittir-PopupDatePicker");
		icon.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				if (pp.isAttached()) {
					pp.hide();
				} else {
					int width = Window.getClientWidth()
							+ Window.getScrollLeft();
					pp.setPopupPosition(getAbsoluteLeft(), getAbsoluteTop()
							+ getOffsetHeight());
					base.addCalendarListener(new CalendarListener() {
						public boolean onDateClicked(Calendar calendar,
								Date date) {
							if (date.getMonth() != base.getRenderDate()
									.getMonth()
									|| date.getYear() != base.getRenderDate()
											.getYear()) {
								return true;
							}
							pp.hide();
							calendar.removeCalendarListener(this);
							return true;
						}
					});
					pp.show();
					if (pp.getPopupLeft() + base.getOffsetWidth() > width) {
						pp.setPopupPosition(pp.getPopupLeft()
								+ (width - pp.getPopupLeft() - base
										.getOffsetWidth()), pp.getPopupTop());
					}
				}
			}
		});
		this.base.addPropertyChangeListener("value",
				new PropertyChangeListener() {
					public void propertyChange(PropertyChangeEvent evt) {
						changes.firePropertyChange("value", evt.getOldValue(),
								evt.getNewValue());
					}
				});
		this.initWidget(holder);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		// TODO Auto-generated method stub
		super.addPropertyChangeListener(l);
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener l) {
		// TODO Auto-generated method stub
		super.addPropertyChangeListener(propertyName, l);
	}

	/**
	 * Current Date value.
	 * 
	 * @return Current Date value.
	 */
	public Object getValue() {
		return this.base.getValue();
	}

	/**
	 * Current Date value.
	 * 
	 * @param value
	 *            Current Date value.
	 */
	public void setValue(Object value) {
		if (value != null) {
			this.base.setValue(value);
		}
	}

	/**
	 * 
	 * @param cdl
	 */
	public void addCalendarDrawListener(CalendarDrawListener cdl) {
		this.base.addCalendarDrawListener(cdl);
	}

	/**
	 * 
	 * @param cdl
	 */
	public void removeCalendarDrawListener(CalendarDrawListener cdl) {
		this.base.removeCalendarDrawListener(cdl);
	}

	/**
	 * 
	 * @return CalendarDrawListeners
	 */
	public CalendarDrawListener[] getCalendarDrawListeners() {
		return this.base.getCalendarDrawListeners();
	}

	/**
	 * 
	 * @param l
	 */
	public void addCalendarListener(CalendarListener l) {
		this.base.addCalendarListener(l);
	}

	/**
	 * 
	 * @param l
	 */
	public void removeCalendarListener(CalendarListener l) {
		this.base.removeCalendarListener(l);
	}

	/**
	 * 
	 * @return CalendarDrawListeners
	 */
	public CalendarListener[] getCalendarListeners() {
		return this.base.getCalendarListeners();
	}

	/**
	 * Gets the current Renderer. Defaults to Renderers.SHORT_DATE_RENDERER.
	 * 
	 * @return Current Renderer
	 */
	public Renderer getRenderer() {
		return this.textBox.getRenderer();
	}

	/**
	 * Sets the current Renderer. Defaults to Renderers.SHORT_DATE_RENDERER.
	 * 
	 * @param renderer
	 *            Renderer to use.
	 */
	public void setRenderer(Renderer renderer) {
		this.textBox.setRenderer(renderer);
	}

	

	public void addClickListener(ClickListener listener) {
		this.icon.addClickListener(listener);
	}

	public void removeClickListener(ClickListener listener) {
		this.icon.removeClickListener(listener);
	}

	
}
