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

import java.util.Comparator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.action.Action;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.SimpleComparator;

import cc.alcina.framework.common.client.util.CommonUtils;

@SuppressWarnings("deprecation")
/**
 *
 * @author Nick Reddel
 */
public class PasswordTextBox<B> extends AbstractBoundWidget<String> implements
		HasFocus, HasEnabled, SourcesKeyboardEvents, SourcesClickEvents {
	private com.google.gwt.user.client.ui.PasswordTextBox base = new com.google.gwt.user.client.ui.PasswordTextBox();

	private ChangeListenerCollection changeListeners = new ChangeListenerCollection();

	private String old;

	public PasswordTextBox() {
		this(false);
	}

	/** Creates a new instance of TextBox */
	public PasswordTextBox(final boolean updateOnKeypress) {
		final PasswordTextBox instance = this;
		old = base.getText();
		this.setComparator(SimpleComparator.INSTANCE);
		if (updateOnKeypress) {
			this.addKeyboardListener(new KeyboardListener() {
				@Override
				public void onKeyDown(Widget sender, char keyCode,
						int modifiers) {
				}

				@Override
				public void onKeyPress(Widget sender, char keyCode,
						int modifiers) {
					changes.firePropertyChange("value", old, getValue());
					old = (String) getValue();
				}

				@Override
				public void onKeyUp(Widget sender, char keyCode,
						int modifiers) {
				}
			});
		} else {
			this.addKeyboardListener(new KeyboardListener() {
				@Override
				public void onKeyDown(Widget sender, char keyCode,
						int modifiers) {
				}

				@Override
				public void onKeyPress(Widget sender, char keyCode,
						int modifiers) {
					if (keyCode == KeyboardListener.KEY_ENTER) {
						setFocus(false);
						setFocus(true);
					}
				}

				@Override
				public void onKeyUp(Widget sender, char keyCode,
						int modifiers) {
				}
			});
		}
		this.base.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(Widget sender) {
				changes.firePropertyChange("value", old, getValue());
				old = (String) getValue();
				changeListeners.fireChange(instance);
			}
		});
		super.initWidget(this.base);
	}

	public void addChangeListener(ChangeListener listener) {
		this.base.addChangeListener(listener);
	}

	@Override
	public void addClickListener(ClickListener listener) {
		this.base.addClickListener(listener);
	}

	@Override
	public void addFocusListener(FocusListener listener) {
		this.base.addFocusListener(listener);
	}

	@Override
	public void addKeyboardListener(KeyboardListener listener) {
		this.base.addKeyboardListener(listener);
	}

	public void cancelKey() {
		this.base.cancelKey();
	}

	@Override
	public Action getAction() {
		Action retValue;
		retValue = super.getAction();
		return retValue;
	}

	@Override
	public Comparator getComparator() {
		Comparator retValue;
		retValue = super.getComparator();
		return retValue;
	}

	public int getCursorPos() {
		int retValue;
		retValue = this.base.getCursorPos();
		return retValue;
	}

	public int getMaxLength() {
		int retValue;
		retValue = this.base.getMaxLength();
		return retValue;
	}

	@Override
	public Object getModel() {
		Object retValue;
		retValue = super.getModel();
		return retValue;
	}

	public String getName() {
		String retValue;
		retValue = this.base.getName();
		return retValue;
	}

	@Override
	public int getOffsetHeight() {
		int retValue;
		retValue = this.base.getOffsetHeight();
		return retValue;
	}

	@Override
	public int getOffsetWidth() {
		int retValue;
		retValue = this.base.getOffsetWidth();
		return retValue;
	}

	public String getSelectedText() {
		String retValue;
		retValue = this.base.getSelectedText();
		return retValue;
	}

	public int getSelectionLength() {
		int retValue;
		retValue = this.base.getSelectionLength();
		return retValue;
	}

	@Override
	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	@Override
	public int getTabIndex() {
		return this.base.getTabIndex();
	}

	public String getText() {
		return this.base.getText();
	}

	@Override
	public String getTitle() {
		return this.base.getTitle();
	}

	@Override
	public String getValue() {
		try {
			return this.base.getText().length() == 0 ? null
					: this.base.getText();
		} catch (RuntimeException re) {
			GWT.log("" + this.base, re);
			return null;
		}
	}

	public int getVisibleLength() {
		return this.base.getVisibleLength();
	}

	@Override
	public boolean isEnabled() {
		return this.base.isEnabled();
	}

	public void removeChangeListener(ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	@Override
	public void removeClickListener(ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	@Override
	public void removeFocusListener(FocusListener listener) {
		this.base.removeFocusListener(listener);
	}

	@Override
	public void removeKeyboardListener(KeyboardListener listener) {
		this.base.removeKeyboardListener(listener);
	}

	@Override
	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	public void selectAll() {
		this.base.selectAll();
	}

	@Override
	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	@Override
	public void setAction(Action action) {
		super.setAction(action);
	}

	public void setCursorPos(int pos) {
		this.base.setCursorPos(pos);
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.base.setEnabled(enabled);
	}

	@Override
	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	@Override
	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setKey(char key) {
		this.base.setKey(key);
	}

	public void setMaxLength(int length) {
		this.base.setMaxLength(length);
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
	}

	public void setName(String name) {
		this.base.setName(name);
	}

	@Override
	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	public void setReadOnly(boolean readOnly) {
	}

	public void setSelectionRange(int pos, int length) {
		this.base.setSelectionRange(pos, length);
	}

	@Override
	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	@Override
	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	@Override
	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}

	public void setText(String text) {
		this.base.setText(text);
	}

	public void setTextAlignment(TextBoxBase.TextAlignConstant align) {
		this.base.setTextAlignment(align);
	}

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	@Override
	public void setValue(String value) {
		String old = this.getValue();
		this.setText(CommonUtils.nullToEmpty(value));
		if (this.getValue() != old && (this.getValue() == null
				|| (this.getValue() != null && !this.getValue().equals(old)))) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	public void setVisibleLength(int length) {
		this.base.setVisibleLength(length);
	}

	@Override
	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	@Override
	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	@Override
	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}
}
