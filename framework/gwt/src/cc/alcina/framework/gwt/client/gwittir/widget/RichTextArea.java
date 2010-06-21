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

import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.richtext.RichTextToolbar;

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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.SimpleComparator;

/**
 *
 * @author Nick Reddel
 */
public class RichTextArea extends AbstractBoundWidget<String> implements
		Focusable, HasAllKeyHandlers, HasAllFocusHandlers, HasClickHandlers {
	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	private com.google.gwt.user.client.ui.RichTextArea base = new com.google.gwt.user.client.ui.RichTextArea();

	public com.google.gwt.user.client.ui.RichTextArea getBase() {
		return this.base;
	}

	private RichTextToolbar toolbar = createToolbar();

	protected RichTextToolbar createToolbar() {
		return new RichTextToolbar(base);
	}

	public String getTitle() {
		return this.base.getTitle();
	}

	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	private String old;

	@SuppressWarnings("unchecked")
	public RichTextArea() {
		old = base.getHTML();
		this.setComparator(SimpleComparator.INSTANCE);
		this.base.addBlurHandler(new BlurHandler() {
			public void onBlur(BlurEvent event) {
				ClientLayerLocator.get().notifications().log("lostfocus");
				EventTarget eventTarget = event.getNativeEvent()
						.getEventTarget();
				Node elt = Node.as(eventTarget);
				if (WidgetUtils.isAncestorOf(toolbar.getElement(), elt)) {
					return;
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
		FlowPanel fp = new FlowPanel();
		FlowPanel tbHolder = new FlowPanel();
		tbHolder.setStyleName("alcina-richTextToolbarBkg");
		tbHolder.add(toolbar);
		fp.add(tbHolder);
		fp.add(base);
		super.initWidget(fp);
	}

	private boolean maximised = false;

	public String getValue() {
		return base.getHTML();
	}

	public void setValue(String value) {
		old = this.getValue();
		if (value != old && value != null && !value.equals(old)) {
			base.setHTML(value);
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	@Override
	protected void onDetach() {
		super.onDetach();
	}


	public int getTabIndex() {
		return this.base.getTabIndex();
	}


	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
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
}
