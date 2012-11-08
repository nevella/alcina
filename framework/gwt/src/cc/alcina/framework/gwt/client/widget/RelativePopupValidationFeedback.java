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
package cc.alcina.framework.gwt.client.widget;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import cc.alcina.framework.common.client.gwittir.validator.ServerValidator.ProcessingServerValidationException;
import cc.alcina.framework.gwt.client.logic.WidgetByElementTracker;
import cc.alcina.framework.gwt.client.util.DomUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.validator.AbstractValidationFeedback;
import com.totsp.gwittir.client.validator.ValidationException;
import com.totsp.gwittir.client.validator.ValidationFeedback;

/**
 * 
 * @author Nick Reddel
 */
public class RelativePopupValidationFeedback extends AbstractValidationFeedback {
	public static final String CONTEXT_FEEDBACK_POSITION = RelativePopupValidationFeedback.class
			.getName() + ".CONTEXT_FEEDBACK_POSITION";

	public static final int LEFT = 1;

	public static final int TOP = 2;

	public static final int RIGHT = 3;

	public static final int BOTTOM = 4;

	final HashMap popups = new HashMap();

	final HashMap listeners = new HashMap();

	private int position;

	private String css;

	private boolean asHtml;

	public void addCssBackground() {
		css = (css == null ? "" : css + " ") + "withBkg";
	}

	/** Creates a new instance of PopupValidationFeedback */
	public RelativePopupValidationFeedback(int position) {
		this.position = position;
	}

	public RelativePopupValidationFeedback(int position,
			ValidationFeedback feedback) {
		this(position);
		if (feedback instanceof AbstractValidationFeedback) {
			getMappings().putAll(
					((AbstractValidationFeedback) feedback).getMappings());
		}
	}

	class RelativePopup extends FlowPanel {
		@Override
		protected void onDetach() {
			super.onDetach();
		}
	}

	@SuppressWarnings("unchecked")
	protected Widget renderExceptionWidget(ValidationException exception) {
		return asHtml ? new HTML(this.getMessage(exception)) : new Label(
				this.getMessage(exception));
	}

	public void handleException(Object source, ValidationException exception) {
		final Widget w = (Widget) source;
		resolve(source);
		if (!DomUtils.isVisibleAncestorChain(w.getElement())) {
			return;
		}
		final RelativePopup p = new RelativePopup();
		p.setVisible(false);
		popups.put(source, p);
		WidgetByElementTracker.get().register(p);
		p.setStyleName("gwittir-ValidationPopup");
		if (getCss() != null) {
			p.addStyleName(getCss());
		}
		if (exception instanceof ProcessingServerValidationException) {
			ProcessingServerValidationException psve = (ProcessingServerValidationException) exception;
			FlowPanel fp = new FlowPanel();
			fp.setStyleName("gwittir-ServerValidation");
			fp.add(new InlineLabel(this.getMessage(exception)));
			p.add(fp);
			psve.setSourceWidget(source);
			psve.setFeedback(this);
		} else {
			p.add(renderExceptionWidget(exception));
		}
		int x = w.getAbsoluteLeft();
		int y = w.getAbsoluteTop();
		Widget pw = WidgetUtils.getPositioningParent(w);
		ComplexPanel cp = WidgetUtils.complexChildOrSelf(pw);
		if (!(pw instanceof RootPanel)) {
			x -= pw.getAbsoluteLeft();
			y -= pw.getAbsoluteTop();
		}
		cp.add(p);
		if (this.position == BOTTOM) {
			y += w.getOffsetHeight();
		} else if (this.position == RIGHT) {
			x += w.getOffsetWidth();
		} else if (this.position == LEFT) {
			x -= p.getOffsetWidth();
		} else if (this.position == TOP) {
			y -= p.getOffsetHeight();
		}
		Element h = p.getElement();
		DOM.setStyleAttribute(h, "position", "absolute");
		DOM.setStyleAttribute(h, "left", x + "px");
		DOM.setStyleAttribute(h, "top", y + "px");
		p.setVisible(true);
		if (w instanceof SourcesPropertyChangeEvents) {
			// GWT.log("is PCE", null);
			PropertyChangeListener attachListener = new PropertyChangeListener() {
				public void propertyChange(
						PropertyChangeEvent propertyChangeEvent) {
					if (((Boolean) propertyChangeEvent.getNewValue())
							.booleanValue()) {
						p.setVisible(true);
					} else {
						p.setVisible(false);
					}
				}
			};
			listeners.put(w, attachListener);
			((SourcesPropertyChangeEvents) w).addPropertyChangeListener(
					"attached", attachListener);
			((SourcesPropertyChangeEvents) w).addPropertyChangeListener(
					"visible", attachListener);
		}
	}

	public void resolve(Object source) {
		RelativePopup p = (RelativePopup) popups.get(source);
		if (p != null) {
			p.removeFromParent();
			popups.remove(source);
			if (listeners.containsKey(source)) {
				try {
					((SourcesPropertyChangeEvents) source)
							.removePropertyChangeListener("attach",
									(PropertyChangeListener) listeners
											.remove(source));
					((SourcesPropertyChangeEvents) source)
							.removePropertyChangeListener("visible",
									(PropertyChangeListener) listeners
											.remove(source));
				} catch (RuntimeException re) {
					re.printStackTrace();
				}
			}
		}
	}

	public void setCss(String css) {
		this.css = css;
	}

	public String getCss() {
		return css;
	}

	public boolean isAsHtml() {
		return this.asHtml;
	}

	public void setAsHtml(boolean asHtml) {
		this.asHtml = asHtml;
	}

	public int getPosition() {
		return this.position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}
