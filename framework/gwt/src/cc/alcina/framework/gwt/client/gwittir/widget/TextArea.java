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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.action.Action;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.HasEnabled;
import com.totsp.gwittir.client.ui.SimpleComparator;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;

@SuppressWarnings("deprecation")
/**
 *
 * @author Nick Reddel
 */
public class TextArea<B> extends AbstractBoundWidget<String>
		implements HasFocus, HasEnabled, SourcesKeyboardEvents,
		SourcesClickEvents, MultilineWidget, Focusable {
	private com.google.gwt.user.client.ui.TextArea base = new com.google.gwt.user.client.ui.TextArea();

	private ChangeListenerCollection changeListeners = new ChangeListenerCollection();

	private String old;

	private boolean ensureAllLinesVisible;

	private String hint;

	private HandlerRegistration keydownHandlerRegistration;

	private HandlerRegistration focusHandlerRegistration;

	public TextArea() {
		this(false);
	}

	/** Creates a new instance of TextBox */
	@SuppressWarnings("unchecked")
	public TextArea(final boolean updateOnKeypress) {
		final TextArea instance = this;
		old = base.getText();
		this.setComparator(SimpleComparator.INSTANCE);
		if (updateOnKeypress) {
			this.addKeyboardListener(new KeyboardListener() {
				public void onKeyDown(Widget sender, char keyCode,
						int modifiers) {
				}

				public void onKeyPress(Widget sender, char keyCode,
						int modifiers) {
					changes.firePropertyChange("value", old, getValue());
					old = (String) getValue();
				}

				public void onKeyUp(Widget sender, char keyCode,
						int modifiers) {
				}
			});
		} else {
		}
		this.base.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				changes.firePropertyChange("value", old, getValue());
				old = (String) getValue();
				changeListeners.fireChange(instance);
			}
		});
		this.base.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				changes.firePropertyChange("value", old, getValue());
				old = (String) getValue();
				changeListeners.fireChange(instance);
			}
		});
		this.base.addChangeListener(new ChangeListener() {
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

	public void addClickListener(ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public void addFocusListener(FocusListener listener) {
		this.base.addFocusListener(listener);
	}

	public void addKeyboardListener(KeyboardListener listener) {
		this.base.addKeyboardListener(listener);
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

	public void cancelKey() {
		this.base.cancelKey();
	}

	public Action getAction() {
		Action retValue;
		retValue = super.getAction();
		return retValue;
	}

	public int getCharacterWidth() {
		return this.base.getCharacterWidth();
	}

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

	public String getHint() {
		return this.hint;
	}

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

	public int getOffsetHeight() {
		int retValue;
		retValue = this.base.getOffsetHeight();
		return retValue;
	}

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

	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	public int getTabIndex() {
		return this.base.getTabIndex();
	}

	public String getText() {
		return this.base.getText();
	}

	public String getTitle() {
		return this.base.getTitle();
	}

	public String getValue() {
		try {
			return this.base.getText().length() == 0 || provideIsHinted() ? null
					: this.base.getText();
		} catch (RuntimeException re) {
			GWT.log("" + this.base, re);
			return null;
		}
	}

	public int getVisibleLines() {
		return this.base.getVisibleLines();
	}

	public boolean isEnabled() {
		return this.base.isEnabled();
	}

	public boolean isEnsureAllLinesVisible() {
		return this.ensureAllLinesVisible;
	}

	public boolean isMultiline() {
		return true;
	}

	public boolean isReadOnly() {
		return this.base.isReadOnly();
	}

	public void removeChangeListener(ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	public void removeClickListener(ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		this.changeListeners.remove(listener);
	}

	public void removeKeyboardListener(KeyboardListener listener) {
		this.base.removeKeyboardListener(listener);
	}

	public void removeStyleName(String style) {
		this.base.removeStyleName(style);
	}

	public void selectAll() {
		this.base.selectAll();
	}

	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setAction(Action action) {
		super.setAction(action);
	}

	public void setCharacterWidth(int width) {
		this.base.setCharacterWidth(width);
	}

	public void setCursorPos(int pos) {
		this.base.setCursorPos(pos);
	}

	public void setEnabled(boolean enabled) {
		this.base.setEnabled(enabled);
	}

	public void setEnsureAllLinesVisible(boolean ensureAllLinesVisible) {
		this.ensureAllLinesVisible = ensureAllLinesVisible;
	}

	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setHint(String hint) {
		if (hint != null && (provideIsHinted()
				|| CommonUtils.isNullOrEmpty(getValue()))) {
			base.setText(hint);
			base.addStyleName("hint");
			keydownHandlerRegistration = base
					.addKeyDownHandler(new KeyDownHandler() {
						@Override
						public void onKeyDown(KeyDownEvent event) {
							clearHint();
						}
					});
			focusHandlerRegistration = base.addFocusHandler(new FocusHandler() {
				@Override
				public void onFocus(FocusEvent event) {
					Scheduler.get().scheduleDeferred(new ScheduledCommand() {
						@Override
						public void execute() {
							clearHint();
							base.setCursorPos(0);
						}
					});
				};
			});
		}
		this.hint = hint;
	}

	public void setKey(char key) {
		this.base.setKey(key);
	}

	public void setModel(Object model) {
		super.setModel(model);
	}

	public void setName(String name) {
		this.base.setName(name);
	}

	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	public void setReadOnly(boolean readOnly) {
		this.base.setReadOnly(readOnly);
	}

	public void setSelectionRange(int pos, int length) {
		this.base.setSelectionRange(pos, length);
	}

	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}

	public void setText(String text) {
		this.base.setText(text);
	}

	public void setTextAlignment(TextBoxBase.TextAlignConstant align) {
		this.base.setTextAlignment(align);
	}

	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setValue(String value) {
		setStyleName("empty", CommonUtils.isNullOrEmpty(value));
		if (provideIsHinted()) {
			if (CommonUtils.isNullOrEmpty(value)) {
				return;
			} else {
				removeStyleName("hint");
			}
		}
		String old = this.getValue();
		this.setText(value);
		if (ensureAllLinesVisible) {
			Scheduler.get().scheduleDeferred(() -> {
				Element element = this.base.getElement();
				element.getStyle().setProperty("height", "auto");
				int scrollHeight = element.getScrollHeight();
				element.getStyle().setHeight(scrollHeight, Unit.PX);
			});
		}
		if (this.getValue() != old && (this.getValue() == null
				|| (this.getValue() != null && !this.getValue().equals(old)))) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
		if (CommonUtils.isNullOrEmpty(value)) {
			setHint(hint);
		}
	}

	public void setVisibleLines(int lines) {
		this.base.setVisibleLines(lines);
	}

	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	public void sinkEvents(int eventBitsToAdd) {
		this.base.sinkEvents(eventBitsToAdd);
	}

	public void unsinkEvents(int eventBitsToRemove) {
		this.base.unsinkEvents(eventBitsToRemove);
	}

	private void clearHint() {
		base.setText(getValue());
		base.removeStyleName("hint");
		if (keydownHandlerRegistration != null) {
			keydownHandlerRegistration.removeHandler();
			focusHandlerRegistration.removeHandler();
			keydownHandlerRegistration = null;
			focusHandlerRegistration = null;
		}
	}

	protected boolean provideIsHinted() {
		return hint != null && hint.equals(this.base.getText());
	}
}
