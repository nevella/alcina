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

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author Nick Reddel
 */
public class APanel<O> extends ComplexPanel
		implements HasClickHandlers, FluidWidget<APanel> {
	/**
	 * Creates an empty A panel. Note: adding invalid (block) elements/widgets
	 * may cause funkeh IE errors
	 */
	private boolean enabled = true;

	private O userObject;

	private boolean bubbling = true;

	private boolean preventDefault = false;

	public APanel() {
		setElement(DOM.createAnchor());
		getElement().setPropertyString("href", "#");
		sinkEvents(Event.ONCLICK);
	}

	public APanel(Widget w) {
		this();
		add(w);
	}

	/**
	 * Adds a new child widget to the panel.
	 * 
	 * @param w
	 *            the widget to be added
	 */
	@Override
	public void add(Widget w) {
		add(w, getElement());
	}

	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public String getHref() {
		return getElement().getPropertyString("href");
	}

	public String getTarget() {
		return getElement().getPropertyString("target");
	}

	public String getType() {
		return getElement().getPropertyString("type");
	}

	public O getUserObject() {
		return userObject;
	}

	/**
	 * Inserts a widget before the specified index.
	 * 
	 * @param w
	 *            the widget to be inserted
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 * @throws IndexOutOfBoundsException
	 *             if <code>beforeIndex</code> is out of range
	 */
	public void insert(Widget w, int beforeIndex) {
		insert(w, getElement(), beforeIndex, true);
	}

	public boolean isBubbling() {
		return bubbling;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isPreventDefault() {
		return preventDefault;
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONCLICK) {
			if (getHref().startsWith("#") || preventDefault) {
				event.preventDefault();
			}
			if (!bubbling) {
				DOM.eventCancelBubble(event, true);
			}
			if (enabled) {
				super.onBrowserEvent(event);
			}
		}
	}

	public void setBubbling(boolean bubbling) {
		this.bubbling = bubbling;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		if (enabled) {
			removeStyleName("disabled");
		} else {
			addStyleName("disabled");
		}
	}

	public void setHref(String href) {
		getElement().setPropertyString("href", href);
	}

	public void setPreventDefault(boolean preventDefault) {
		this.preventDefault = preventDefault;
	}

	public void setTarget(String target) {
		getElement().setPropertyString("target", target);
	}

	public void setType(String type) {
		getElement().setPropertyString("type", type);
	}

	public void setUserObject(O userObject) {
		this.userObject = userObject;
	}

	public static class AAnchor extends APanel {
		private Label label = null;

		public AAnchor(String text, boolean html, String token) {
			super();
			setLabel(html ? new InlineHTML(text) : new InlineLabel(text));
			setHref(token);
		}

		public AAnchor(String text, String token) {
			super();
			setLabel(new InlineLabel(text));
			setHref(token);
		}

		public Label getLabel() {
			return this.label;
		}

		public void setLabel(Label label) {
			this.label = label;
			clear();
			add(label);
		}
	}

	public static class UnfocusableAPanel<O> extends APanel<O> {
		public UnfocusableAPanel() {
			super();
		}

		public UnfocusableAPanel(Widget w) {
			super(w);
		}

		/**
		 * Copied from Safari focusimpl
		 * 
		 * @param elem
		 */
		public native void blur(Element elem) /*-{
      // Attempts to blur elements from within an event callback will generally
      // be unsuccessful, so we invoke blur() from outside of the callback.
      $wnd.setTimeout(function() {
        elem.blur();
      }, 0);
		}-*/;

		@Override
		public void onBrowserEvent(Event event) {
			super.onBrowserEvent(event);
			if (DOM.eventGetType(event) == Event.ONCLICK) {
				blur(getElement());
			}
		}
	}
}
