/*
 * ListBox.java
 *
 * Created on July 5, 2007, 6:12 PM
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
package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.log.Level;
import com.totsp.gwittir.client.log.Logger;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.SimpleComparator;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.common.client.util.CommonUtils;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
@SuppressWarnings("deprecation")
public class SingleSelectionListBox<T> extends AbstractBoundWidget<T>
		implements HasFocus, SourcesFocusEvents, SourcesChangeEvents {
	public static final String VALUE_PROPERTY_NAME = "value";

	private static final Logger LOGGER = Logger
			.getLogger(SingleSelectionListBox.class.toString());

	private com.google.gwt.user.client.ui.ListBox base;

	private T selected = null;

	private Collection<T> options = new ArrayList<T>();

	private Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();

	private Renderer<T, String> renderer;

	/** Creates a new instance of ListBox */
	public SingleSelectionListBox() {
		super();
		this.base = new com.google.gwt.user.client.ui.ListBox();
		this.setRenderer((Renderer<T, String>) ToStringRenderer.INSTANCE);
		this.setComparator(SimpleComparator.INSTANCE);
		this.base.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				update();
			}
		});
		this.base.addChangeListener(new ChangeListener() {
			public void onChange(Widget sender) {
				update();
			}
			// foo!
		});
		super.initWidget(base);
	}

	public void addChangeListener(final ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	public void addClickListener(final ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public void addFocusListener(final FocusListener listener) {
		this.base.addFocusListener(listener);
	}

	public void addItem(final T o) {
		options.add(o);
		this.base.addItem(this.getRenderer().render(o));
	}

	public void addKeyboardListener(KeyboardListener listener) {
		this.base.addKeyboardListener(listener);
	}

	public void addStyleName(final String style) {
		this.base.addStyleName(style);
	}

	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		try {
			final SingleSelectionListBox<?> other = (SingleSelectionListBox<?>) obj;
			if ((this.options != other.options) && ((this.options == null)
					|| !this.options.equals(other.options))) {
				return false;
			}
			return true;
		} catch (ClassCastException e) {
			return false;
		}
	}

	public int getAbsoluteLeft() {
		int retValue;
		retValue = this.base.getAbsoluteLeft();
		return retValue;
	}

	public int getAbsoluteTop() {
		int retValue;
		retValue = this.base.getAbsoluteTop();
		return retValue;
	}

	public int getItemCount() {
		int retValue;
		retValue = this.base.getItemCount();
		return retValue;
	}

	public String getItemText(int index) {
		String retValue;
		retValue = this.base.getItemText(index);
		return retValue;
	}

	public String getName() {
		return this.base.getName();
	}

	@Override
	public int getOffsetHeight() {
		return this.base.getOffsetHeight();
	}

	public int getOffsetWidth() {
		return this.base.getOffsetWidth();
	}

	public Collection<T> getOptions() {
		return options;
	}

	/**
	 * Get the value of renderer
	 *
	 * @return the value of renderer
	 */
	public Renderer<T, String> getRenderer() {
		return this.renderer;
	}

	public int getSelectedIndex() {
		return this.base.getSelectedIndex();
	}

	@Override
	public String getStyleName() {
		String retValue;
		retValue = this.base.getStyleName();
		return retValue;
	}

	public int getTabIndex() {
		int retValue;
		retValue = this.base.getTabIndex();
		return retValue;
	}

	@Override
	public String getTitle() {
		return this.base.getTitle();
	}

	public T getValue() {
		SingleSelectionListBox.LOGGER.log(Level.SPAM,
				"IsMultipleSelect. Returning collection", null);
		return this.selected;
	}

	public int getVisibleItemCount() {
		int retValue;
		retValue = this.base.getVisibleItemCount();
		return retValue;
	}

	public int hashCode() {
		return this.base.hashCode();
	}

	public boolean isEnabled() {
		boolean retValue;
		retValue = this.base.isEnabled();
		return retValue;
	}

	public boolean isItemSelected(int index) {
		boolean retValue;
		retValue = this.base.isItemSelected(index);
		return retValue;
	}

	public boolean isMultipleSelect() {
		return this.base.isMultipleSelect();
	}

	public void removeChangeListener(final ChangeListener listener) {
		this.changeListeners.remove(listener);
	}

	public void removeClickListener(final ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	public void removeFocusListener(final FocusListener listener) {
		this.base.removeFocusListener(listener);
	}

	public void removeItem(final int index) {
		this.base.removeItem(index);
	}

	public void removeItem(final Object o) {
		int i = 0;
		for (Iterator<T> it = this.options.iterator(); it.hasNext(); i++) {
			T option = it.next();
			if (this.getComparator().compare(option, o) == 0) {
				this.options.remove(option);
				this.base.removeItem(i);
				this.update();
			}
		}
	}

	public void removeKeyboardListener(final KeyboardListener listener) {
		this.base.removeKeyboardListener(listener);
	}

	@Override
	public void removeStyleName(final String style) {
		this.base.removeStyleName(style);
	}

	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
	}

	public void setEnabled(boolean enabled) {
		this.base.setEnabled(enabled);
	}

	public void setFocus(boolean focused) {
		this.base.setFocus(focused);
	}

	public void setHeight(String height) {
		this.base.setHeight(height);
	}

	public void setItemText(int index, String text) {
		this.base.setItemText(index, text);
	}

	public void setMultipleSelect(boolean multiple) {
		this.base.setMultipleSelect(multiple);
	}

	public void setName(String name) {
		this.base.setName(name);
	}

	public void setOptions(Collection<T> options) {
		this.options = new ArrayList<T>();
		base.clear();
		ArrayList<T> newSelected = new ArrayList<T>();
		T old = this.selected;
		for (Iterator<T> it = options.iterator(); it.hasNext();) {
			T item = it.next();
			this.base.addItem(this.getRenderer().render(item));
			if (CommonUtils.equalsWithNullEquality(item, this.selected)) {
				this.base.setItemSelected(this.base.getItemCount() - 1, true);
				this.selected = item;
			}
			this.options.add(item);
		}
		changes.firePropertyChange(VALUE_PROPERTY_NAME, old, selected);
		fireChangeListeners();
	}

	@Override
	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	public void setRenderer(Renderer<T, String> renderer) {
		this.renderer = renderer;
		this.setOptions(this.options);
	}

	@Override
	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	@Override
	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	public void setValue(T value) {
		int i = 0;
		T old = this.selected;
		this.selected = value;
		for (Iterator<T> it = this.options.iterator(); it.hasNext(); i++) {
			T item = it.next();
			if (CommonUtils.equalsWithNullEquality(selected, item)) {
				base.setItemSelected(i, true);
				this.selected = item;
			} else {
				base.setItemSelected(i, false);
			}
		}
		changes.firePropertyChange(VALUE_PROPERTY_NAME, old, selected);
		fireChangeListeners();
	}

	public void setVisibleItemCount(final int visibleItems) {
		this.base.setVisibleItemCount(visibleItems);
	}

	public void setWidth(final String width) {
		this.base.setWidth(width);
	}

	private void fireChangeListeners() {
		for (Iterator<ChangeListener> it = this.changeListeners.iterator(); it
				.hasNext();) {
			ChangeListener l = it.next();
			l.onChange(this);
		}
		if (this.getAction() != null) {
			this.getAction().execute(this);
		}
	}

	private void update() {
		T old = this.selected;
		Iterator<T> it = this.options.iterator();
		for (int i = 0; (i < base.getItemCount()) && it.hasNext(); i++) {
			T item = it.next();
			if (this.base.isItemSelected(i)) {
				this.selected = item;
			}
		}
		changes.firePropertyChange(VALUE_PROPERTY_NAME, old, this.selected);
		fireChangeListeners();
	}
}
