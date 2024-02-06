/*
 * TextBox.java
 *
 * Created on July 16, 2007, 2:59 PM
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.Comparator;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
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

import cc.alcina.framework.common.client.util.Ax;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a> Modified by Nick to handle some updateonkeypress edgecases
 */
@SuppressWarnings("deprecation")
public class TextBox extends AbstractBoundWidget<String> implements HasFocus,
		HasEnabled, SourcesKeyboardEvents, SourcesClickEvents {
	private com.google.gwt.user.client.ui.TextBox base = new com.google.gwt.user.client.ui.TextBox();

	private ChangeListenerCollection changeListeners = new ChangeListenerCollection();

	private String old;

	private String emptyValue;

	public TextBox() {
		this(false);
	}

	/** Creates a new instance of TextBox */
	public TextBox(final boolean updateOnKeypress) {
		final TextBox instance = this;
		old = base.getText();
		if (updateOnKeypress) {
			this.addKeyboardListener(new KeyboardListener() {
				boolean scheduled = false;

				@Override
				public void onKeyDown(Widget sender, char keyCode,
						int modifiers) {
				}

				@Override
				public void onKeyPress(Widget sender, char keyCode,
						int modifiers) {
					refresh();
				}

				@Override
				public void onKeyUp(Widget sender, char keyCode,
						int modifiers) {
					refresh();
				}

				private void refresh() {
					if (!scheduled) {
						Scheduler.get()
								.scheduleDeferred(new ScheduledCommand() {
									@Override
									public void execute() {
										fireChangeFromOld();
										old = (String) getValue();
										scheduled = false;
									}
								});
						scheduled = true;
					}
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
					if (keyCode == KeyCodes.KEY_ENTER) {
						setFocus(false);
						setFocus(true);
						setValue(getValue());
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
				fireChangeFromOld();
				changeListeners.fireChange(instance);
			}
		});
		this.base.addValueChangeHandler(new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				fireChangeFromOld();
				changeListeners.fireChange(instance);
			}
		});
		this.base.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (old == null && getValue() == null) {
					// don't want a non-change change fired here (invalid
					// validation)
				} else {
					fireChangeFromOld();
				}
				changeListeners.fireChange(instance);
			}
		});
		super.initWidget(this.base);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return this.base.addClickHandler(handler);
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

	private void fireChangeFromOld() {
		changes.firePropertyChange("value", old, getValue());
		old = (String) getValue();
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
			return this.base.getText().length() == 0 ? emptyValue
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
		if (Ax.isBlank(value)) {
			emptyValue = value;
		}
		old = this.getValue();
		this.setText(value);
		// if( this.getValue() != old && this.getValue() != null &&
		// !this.getValue().equals( old ) ){
		// the above doesn't fire a change on the case new==null, old!=null
		if (this.getValue() != old && (this.getValue() == null
				|| (this.getValue() != null && !this.getValue().equals(old)))) {
			fireChangeFromOld();
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
