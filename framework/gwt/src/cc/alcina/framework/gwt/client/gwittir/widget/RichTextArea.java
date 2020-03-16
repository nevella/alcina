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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.InitializeEvent;
import com.google.gwt.event.logical.shared.InitializeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.SimpleComparator;

import cc.alcina.framework.gwt.client.util.DomUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.richtext.RichTextToolbar;

/**
 *
 * @author Nick Reddel
 */
public class RichTextArea extends AbstractBoundWidget<String> implements
		Focusable, HasAllKeyHandlers, HasAllFocusHandlers, HasClickHandlers {
	private com.google.gwt.user.client.ui.RichTextArea base = new com.google.gwt.user.client.ui.RichTextArea();

	private RichTextToolbar toolbar = createToolbar();

	private String old;

	private final String defaultFontSize;

	private boolean maximised = false;

	
	public RichTextArea() {
		this(true, "12px");
	}

	
	public RichTextArea(boolean withToolbar, String defaultFontSize) {
		this.defaultFontSize = defaultFontSize;
		old = base.getHTML();
		this.setComparator(SimpleComparator.INSTANCE);
		this.base.addBlurHandler(new BlurHandler() {
			public void onBlur(BlurEvent event) {
				EventTarget eventTarget = event.getNativeEvent()
						.getEventTarget();
				if (Node.is(eventTarget)) {
					Node elt = Node.as(eventTarget);
					if (DomUtils.isAncestorOf(toolbar.getElement(), elt)) {
						return;
					}
				}
				changes.firePropertyChange("value", old, getValue());
			}
		});
		this.base.addKeyUpHandler(new KeyUpHandler() {
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeKeyCode();
				if (keyCode == 'M' && event.isControlKeyDown()) {
					if (maximised) {
						RichTextArea.this.removeStyleName("max");
						WidgetUtils.restoreFromMaximise();
					} else {
						WidgetUtils.maximiseWidget(RichTextArea.this);
						RichTextArea.this.addStyleName("max");
					}
					maximised = !maximised;
				}
			}
		});
		this.base.addInitializeHandler(new InitializeHandler() {
			@Override
			public void onInitialize(InitializeEvent event) {
				styleBody(base.getElement(), "12px");
			}
		});
		FlowPanel fp = new FlowPanel();
		if (withToolbar) {
			FlowPanel tbHolder = new FlowPanel();
			tbHolder.setStyleName("alcina-richTextToolbarBkg");
			tbHolder.add(toolbar);
			fp.add(tbHolder);
		}
		fp.add(base);
		super.initWidget(fp);
	}

	public HandlerRegistration addBlurHandler(BlurHandler handler) {
		return this.base.addBlurHandler(handler);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return this.base.addClickHandler(handler);
	}

	public HandlerRegistration addFocusHandler(FocusHandler handler) {
		return this.base.addFocusHandler(handler);
	}

	public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
		return this.base.addKeyDownHandler(handler);
	}

	public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
		return this.base.addKeyPressHandler(handler);
	}

	public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
		return this.base.addKeyUpHandler(handler);
	}

	public com.google.gwt.user.client.ui.RichTextArea getBase() {
		return this.base;
	}

	public int getTabIndex() {
		return this.base.getTabIndex();
	}

	public String getTitle() {
		return this.base.getTitle();
	}

	public String getValue() {
		return base.getHTML();
	}

	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}

	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setValue(String value) {
		old = this.getValue();
		if (value != old && value != null && !value.equals(old)) {
			base.setHTML(value);
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	protected RichTextToolbar createToolbar() {
		return new RichTextToolbar(base);
	}

	@Override
	protected void onDetach() {
		changes.firePropertyChange("value", old, getValue());
		super.onDetach();
	}

	protected native void styleBody(Element elem, String defaultFontSize) /*-{
        if (elem.contentWindow && elem.contentWindow.document
                && elem.contentWindow.document.documentElement) {
            elem.contentWindow.document.documentElement.setAttribute("style",
                    "font-family: Arial; margin: 2px;font-size:"
                            + defaultFontSize);
        }
	}-*/;
}
