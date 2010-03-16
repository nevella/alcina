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
import cc.alcina.framework.gwt.client.widget.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.richtext.RichTextToolbar;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.SimpleComparator;
import com.totsp.gwittir.client.ui.ToStringRenderer;
@SuppressWarnings("deprecation")
/**
 *
 * @author Nick Reddel
 */

 public class RichTextArea extends AbstractBoundWidget<String> implements
		HasFocus ,Focusable{
	public void addFocusListener(FocusListener listener) {
		this.base.addFocusListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		this.base.removeFocusListener(listener);
	}

	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	private com.google.gwt.user.client.ui.RichTextArea base = new com.google.gwt.user.client.ui.RichTextArea();

	public com.google.gwt.user.client.ui.RichTextArea getBase() {
		return this.base;
	}

	private RichTextToolbar toolbar = new RichTextToolbar(base);

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
		this.base.addFocusListener(new FocusListener() {
			public void onFocus(Widget sender) {
			}

			public void onLostFocus(Widget sender) {
				ClientLayerLocator.get().clientBase().log("lostfocus");
				Event evt = Event.getCurrentEvent();
				if (evt != null) {
					Element elt = Event.getCurrentEvent().getTarget();
					String str = elt.getString();
					if (WidgetUtils.isAncestorOf(toolbar.getElement(), elt)) {
						return;
					}
				}
				changes.firePropertyChange("value", old, getValue());
			}
		});
		this.base.addKeyboardListener(new KeyboardListener() {
			public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				if (keyCode == 'M'
						&& (modifiers & KeyboardListener.MODIFIER_CTRL) != 0) {
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

			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				// TODO Auto-generated method stub
			}

			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				// TODO Auto-generated method stub
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

	public void addKeyboardListener(KeyboardListener listener) {
		this.base.addKeyboardListener(listener);
	}

	public int getTabIndex() {
		return this.base.getTabIndex();
	}

	public void removeKeyboardListener(KeyboardListener listener) {
		this.base.removeKeyboardListener(listener);
	}

	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}
}
