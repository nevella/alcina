/*
 * Checkbox.java
 *
 * Created on July 15, 2007, 1:40 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.client.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
@SuppressWarnings("deprecation")
public class Checkbox extends AbstractBoundWidget<Boolean> implements
		HasEnabled, SourcesClickEvents, HasFocus, SourcesKeyboardEvents {
	private com.google.gwt.user.client.ui.CheckBox base;

	/** Creates a new instance of Checkbox */
	public Checkbox() {
		super();
		init(null);
	}

	public Checkbox(String label) {
		super();
		init(label);
	}

	public Checkbox(String label, boolean value) {
		super();
		init(label);
		this.setChecked(value);
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

	@Override
	public void addStyleName(String style) {
		this.base.addStyleName(style);
	}

	@Override
	public int getAbsoluteLeft() {
		return this.base.getAbsoluteLeft();
	}

	@Override
	public int getAbsoluteTop() {
		return this.base.getAbsoluteTop();
	}

	public String getHTML() {
		return this.base.getHTML();
	}

	public String getName() {
		return this.base.getName();
	}

	@Override
	public String getStyleName() {
		return this.base.getStyleName();
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
	public Boolean getValue() {
		return this.isChecked() ? Boolean.TRUE : Boolean.FALSE;
	}

	public boolean isChecked() {
		return this.base.isChecked();
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

	@Override
	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setChecked(boolean checked) {
		this.base.setChecked(checked);
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

	public void setHTML(String html) {
		this.base.setHTML(html);
	}

	public void setName(String name) {
		this.base.setName(name);
	}

	@Override
	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
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

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	@Override
	public void setValue(Boolean value) {
		Boolean old = this.getValue();
		this.setChecked(value);
		if ((old != this.getValue()) && !old.equals(this.getValue())) {
			this.changes.firePropertyChange("value", old, this.getValue());
		}
	}

	@Override
	public void setWidth(String width) {
		this.base.setWidth(width);
	}

	private void init(String label) {
		this.base = new com.google.gwt.user.client.ui.CheckBox(label);
		super.initWidget(this.base);
		this.base.addValueChangeHandler(s -> {
			Boolean checked = s.getValue();
			Boolean old = !checked;
			changes.firePropertyChange("value", old, checked);
		});
	}
}
