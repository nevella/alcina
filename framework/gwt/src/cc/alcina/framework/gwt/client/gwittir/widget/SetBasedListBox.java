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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
/*
 * ListBox.java
 *
 * Created on July 5, 2007, 6:12 PM
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.AbstractBoundCollectionWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.SimpleComparator;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.common.client.actions.InlineButtonHandler;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;
import cc.alcina.framework.gwt.client.gwittir.customiser.ListAddItemHandler;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar.ToolbarButton;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;
import cc.alcina.framework.gwt.client.widget.dialog.Prompter;

/**
 *
 */
@SuppressWarnings({ "unchecked", "deprecation" })
public class SetBasedListBox extends AbstractBoundCollectionWidget implements
		HasFocus, SourcesFocusEvents, SourcesChangeEvents, HasEnabled {
	public static final String VALUE_PROPERTY_NAME = "value";

	private ArrayList selected = new ArrayList();

	private Collection options = new ArrayList();

	protected com.google.gwt.user.client.ui.ListBox base;

	private Vector changeListeners = new Vector();

	private boolean sortOptionsByToString = true;

	private ListAddItemHandler listAddItemHandler;

	private ToolbarButton addButton;

	public SetBasedListBox() {
		super();
		init0();
	}

	/** Creates a new instance of ListBox */
	public SetBasedListBox(ListAddItemHandler listAddItemHandler) {
		super();
		this.listAddItemHandler = listAddItemHandler;
		init0();
	}

	@Override
	public void addChangeListener(final ChangeListener listener) {
		this.changeListeners.add(listener);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public void addClickListener(final ClickListener listener) {
		this.base.addClickListener(listener);
	}

	public HandlerRegistration addFocusHandler(FocusHandler handler) {
		return addDomHandler(handler, FocusEvent.getType());
	}

	@Override
	public void addFocusListener(final FocusListener listener) {
		this.base.addFocusListener(listener);
	}

	public void addItem(final Object o) {
		options.add(o);
		this.base.addItem((String) this.getRenderer().render(o));
	}

	@Override
	public void addKeyboardListener(KeyboardListener listener) {
		this.base.addKeyboardListener(listener);
	}

	public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
		return addDomHandler(handler, MouseDownEvent.getType());
	}

	@Override
	public void addStyleName(final String style) {
		this.base.addStyleName(style);
	}

	protected boolean contains(final Collection c, final Object o) {
		for (Iterator it = c.iterator(); it.hasNext();) {
			Object next = it.next();
			if (safeCompare(o, next) == 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null || !(obj instanceof SetBasedListBox)) {
			return false;
		}
		final SetBasedListBox other = (SetBasedListBox) obj;
		if ((this.options != other.options) && ((this.options == null)
				|| !this.options.equals(other.options))) {
			return false;
		}
		return true;
	}

	private void fireAdaptedChange(String propertyName, List old,
			List selected) {
		changes.firePropertyChange(VALUE_PROPERTY_NAME, new HashSet(old),
				new HashSet(selected));
	}

	private void fireChangeListeners() {
		for (Iterator it = this.changeListeners.iterator(); it.hasNext();) {
			ChangeListener l = (ChangeListener) it.next();
			l.onChange(this);
		}
		if (this.getAction() != null) {
			this.getAction().execute(this);
		}
	}

	@Override
	public int getAbsoluteLeft() {
		int retValue;
		retValue = this.base.getAbsoluteLeft();
		return retValue;
	}

	@Override
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

	public Collection getOptions() {
		return options;
	}

	public int getSelectedIndex() {
		int retValue;
		retValue = this.base.getSelectedIndex();
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
		int retValue;
		retValue = this.base.getTabIndex();
		return retValue;
	}

	@Override
	public String getTitle() {
		String retValue;
		retValue = this.base.getTitle();
		return retValue;
	}

	@Override
	public Object getValue() {
		final Object returnValue;
		if (this.base.isMultipleSelect()) {
			returnValue = this.selected;
		} else if (this.selected.size() == 0) {
			returnValue = null;
		} else {
			returnValue = this.selected.get(0);
		}
		return returnValue;
	}

	public int getVisibleItemCount() {
		int retValue;
		retValue = this.base.getVisibleItemCount();
		return retValue;
	}

	@Override
	public int hashCode() {
		return this.base.hashCode();
	}

	private void init0() {
		this.base = new com.google.gwt.user.client.ui.ListBox();
		this.setRenderer(ToStringRenderer.INSTANCE);
		this.setComparator(SimpleComparator.INSTANCE);
		this.base.addClickListener(new ClickListener() {
			@Override
			public void onClick(Widget sender) {
				update();
			}
		});
		this.base.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(Widget sender) {
				update();
			}
			// foo!
		});
		Widget delegate = base;
		if (listAddItemHandler != null) {
			FlowPanel fp = new FlowPanel();
			fp.setStyleName("nowrap");
			delegate = fp;
			fp.add(base);
			InlineButtonHandler addItemAction = new AddItemHandler();
			addButton = new ToolbarButton(addItemAction, true);
			fp.add(UsefulWidgetFactory.createSpacer(2));
			fp.add(addButton);
		}
		super.initWidget(delegate);
	}

	@Override
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

	public boolean isSortOptionsByToString() {
		return sortOptionsByToString;
	}

	@Override
	protected void onDetach() {
		super.onDetach();
	}

	public Object provideOtherValue() {
		Iterator itr = getOptions().iterator();
		Object value = getValue();
		Object other = null;
		while (other == value && itr.hasNext()) {
			other = itr.next();
		}
		return other;
	}

	@Override
	public void removeChangeListener(final ChangeListener listener) {
		this.changeListeners.remove(listener);
	}

	public void removeClickListener(final ClickListener listener) {
		this.base.removeClickListener(listener);
	}

	@Override
	public void removeFocusListener(final FocusListener listener) {
		this.base.removeFocusListener(listener);
	}

	public void removeItem(final int index) {
		this.base.removeItem(index);
	}

	public void removeItem(final Object o) {
		int i = 0;
		for (Iterator it = this.options.iterator(); it.hasNext(); i++) {
			Object option = it.next();
			if (safeCompare(option, o) == 0) {
				this.options.remove(option);
				this.base.removeItem(i);
				this.update();
			}
		}
	}

	@Override
	public void removeKeyboardListener(final KeyboardListener listener) {
		this.base.removeKeyboardListener(listener);
	}

	@Override
	public void removeStyleName(final String style) {
		this.base.removeStyleName(style);
	}

	public int safeCompare(Object value, Object item) {
		try {
			return this.getComparator().compare(value, item);
		} catch (ClassCastException e) {
			return value.getClass().getName()
					.compareTo(item.getClass().getName());
		}
	}

	@Override
	public void setAccessKey(char key) {
		this.base.setAccessKey(key);
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

	public void setItemText(int index, String text) {
		this.base.setItemText(index, text);
	}

	public void setMultipleSelect(boolean multiple) {
		this.base.setMultipleSelect(multiple);
		if (this.selected.size() > 1) {
			Object o = this.selected.get(0);
			this.selected = new ArrayList();
			this.selected.add(o);
		}
	}

	public void setName(String name) {
		this.base.setName(name);
	}

	public void setOptions(Collection options) {
		this.options = new ArrayList();
		base.clear();
		ArrayList newSelected = new ArrayList();
		TextProvider.get().setDecorated(false);
		TextProvider.get().setTrimmed(true);
		if (isSortOptionsByToString()) {
			options = CommonUtils.sortByStringValue(options);
		}
		for (Iterator it = options.iterator(); it.hasNext();) {
			Object item = it.next();
			this.base.addItem((String) this.getRenderer().render(item));
			if (contains(this.selected, item)) {
				this.base.setItemSelected(this.base.getItemCount() - 1, true);
				newSelected.add(item);
			}
			this.options.add(item);
		}
		TextProvider.get().setDecorated(true);
		TextProvider.get().setTrimmed(false);
		ArrayList old = this.selected;
		this.selected = newSelected;
		if (this.isMultipleSelect()) {
			fireAdaptedChange(VALUE_PROPERTY_NAME, old, selected);
		} else {
			Object prev = ((old == null) || (old.size() == 0)) ? null
					: old.get(0);
			Object curr = (this.selected.size() == 0) ? null
					: this.selected.get(0);
			// ignore null-null changes
			if (prev != curr) {
				changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
			}
		}
		fireChangeListeners();
	}

	@Override
	public void setPixelSize(int width, int height) {
		this.base.setPixelSize(width, height);
	}

	@Override
	public void setRenderer(Renderer renderer) {
		super.setRenderer(renderer);
		this.setOptions(this.options);
	}

	@Override
	public void setSize(String width, String height) {
		this.base.setSize(width, height);
	}

	public void setSortOptionsByToString(boolean sortOptions) {
		this.sortOptionsByToString = sortOptions;
	}

	@Override
	public void setStyleName(String style) {
		this.base.setStyleName(style);
	}

	@Override
	public void setTabIndex(int index) {
		this.base.setTabIndex(index);
	}

	@Override
	public void setTitle(String title) {
		this.base.setTitle(title);
	}

	@Override
	public void setValue(Object value) {
		int i = 0;
		ArrayList old = this.selected;
		this.selected = new ArrayList();
		if (value instanceof Collection) {
			Collection c = (Collection) value;
			for (Iterator it = this.options.iterator(); it.hasNext(); i++) {
				Object item = it.next();
				if (contains(c, item)) {
					base.setItemSelected(i, true);
					this.selected.add(item);
				} else {
					base.setItemSelected(i, false);
				}
			}
		} else {
			for (Iterator it = this.options.iterator(); it.hasNext(); i++) {
				Object item = it.next();
				if (safeCompare(value, item) == 0) {
					base.setItemSelected(i, true);
				} else {
					base.setItemSelected(i, false);
				}
			}
			this.selected.add(value);
		}
		if (this.isMultipleSelect()) {
			fireAdaptedChange(VALUE_PROPERTY_NAME, old, selected);
		} else {
			Object prev = ((old == null) || (old.size() == 0)) ? null
					: old.get(0);
			Object curr = (this.selected.size() == 0) ? null
					: this.selected.get(0);
			if (prev != curr) {
				changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
			}
		}
		fireChangeListeners();
	}

	public void setVisibleItemCount(final int visibleItems) {
		this.base.setVisibleItemCount(visibleItems);
	}

	@Override
	public void setWidth(final String width) {
		this.base.setWidth(width);
	}

	private void update() {
		ArrayList selected = new ArrayList();
		Iterator it = this.options.iterator();
		for (int i = 0; (i < base.getItemCount()) && it.hasNext(); i++) {
			Object item = it.next();
			if (this.base.isItemSelected(i)) {
				selected.add(item);
			}
		}
		ArrayList old = this.selected;
		this.selected = selected;
		if (this.isMultipleSelect()) {
			fireAdaptedChange(VALUE_PROPERTY_NAME, old, selected);
		} else {
			Object prev = ((old == null) || (old.size() == 0)) ? null
					: old.get(0);
			Object curr = (this.selected.size() == 0) ? null
					: this.selected.get(0);
			if (prev == null && curr == null) {
				return;// pcs is not MutablePropertyChangeLister
			}
			changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
		}
		fireChangeListeners();
	}

	private class AddItemHandler extends InlineButtonHandler {
		@Override
		public String getActionName() {
			return "+";
		}

		@Override
		public void onClick(ClickEvent event) {
			String validationMessage = listAddItemHandler
					.validateCanAdd(getValue());
			if (validationMessage != null) {
				Registry.impl(ClientNotifications.class)
						.showMessage(validationMessage);
				return;
			}
			String namePrompt = listAddItemHandler.getPrompt();
			String nameValue = null;
			Callback<String> actionCallback = new Callback<String>() {
				@Override
				public void accept(String nameValue) {
					Object newItem = listAddItemHandler
							.createNewItem(nameValue);
					List optionsCopy = new ArrayList(getOptions());
					optionsCopy.add(newItem);
					setOptions(optionsCopy);
					setValue(newItem);
				}
			};
			Callback<OkCancelDialogBox> positioningCallback = new Callback<OkCancelDialogBox>() {
				@Override
				public void accept(OkCancelDialogBox box) {
					box.setPopupPosition(addButton.getAbsoluteLeft(),
							addButton.getAbsoluteTop()
									+ addButton.getOffsetHeight());
				}
			};
			if (namePrompt != null) {
				String defaultName = listAddItemHandler.getDefaultName();
				new Prompter("Message", namePrompt, defaultName, null,
						positioningCallback, actionCallback);
			} else {
				actionCallback.accept(null);
			}
		}
	}

	public static class DomainListBox extends SetBasedListBox {
		private Class domainClass;

		private Predicate predicate;

		private boolean hasNullOption;

		private boolean refreshOnModelChange;

		private SourcesPropertyChangeEvents listenedModel;

		private PropertyChangeListener refreshListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				refreshOptions();
			}
		};

		public DomainListBox(Class domainClass, Predicate filter,
				boolean hasNullOption, ListAddItemHandler addHandler) {
			super(addHandler);
			this.domainClass = domainClass;
			this.predicate = filter;
			this.hasNullOption = hasNullOption;
			if (!(filter instanceof RequiresContextBindable)) {
				refreshOptions();
			}
		}

		private void ensureModelListener(boolean add) {
			if (listenedModel != null) {
				listenedModel.removePropertyChangeListener(refreshListener);
				listenedModel = null;
			}
			if (add) {
				if (getModel() instanceof SourcesPropertyChangeEvents
						&& isAttached() && isRefreshOnModelChange()) {
					listenedModel = (SourcesPropertyChangeEvents) getModel();
					listenedModel.addPropertyChangeListener(refreshListener);
				}
			}
		}

		public Predicate getPredicate() {
			return this.predicate;
		}

		public boolean isHasNullOption() {
			return this.hasNullOption;
		}

		public boolean isRefreshOnModelChange() {
			return this.refreshOnModelChange;
		}

		@Override
		protected void onAttach() {
			super.onAttach();
			ensureModelListener(true);
		}

		@Override
		protected void onDetach() {
			super.onDetach();
			ensureModelListener(false);
		}

		public void refreshOptions() {
			Collection<HasId> collection = TransformManager.get()
					.getCollection(domainClass);
			ArrayList options = new ArrayList();
			if (predicate == null) {
				options.addAll(collection);
			} else {
				if (predicate instanceof RequiresContextBindable) {
					((RequiresContextBindable) predicate).setBindable(
							(SourcesPropertyChangeEvents) getModel());
				}
				Iterator itr = collection.iterator();
				while (itr.hasNext()) {
					Object obj = itr.next();
					if (predicate.test(obj)) {
						options.add(obj);
					}
				}
			}
			if (!isSortOptionsByToString()) {
				if (getComparator() != null) {
					Collections.sort(options, getComparator());
				} else {
					if (!options.isEmpty()
							&& options.get(0) instanceof Comparable) {
						Collections.sort(options);
					}
				}
			}
			if (hasNullOption) {
				options.add(0, null);
			}
			setOptions(options);
		}

		public void setHasNullOption(boolean hasNullOption) {
			this.hasNullOption = hasNullOption;
			refreshOptions();
		}

		@Override
		public void setModel(Object model) {
			super.setModel(model);
			if (model != null) {
				refreshOptions();
			}
			ensureModelListener(true);
		}

		public void setPredicate(Predicate predicate) {
			this.predicate = predicate;
			refreshOptions();
		}

		public void setRefreshOnModelChange(boolean refreshOnModelChange) {
			this.refreshOnModelChange = refreshOnModelChange;
		}
	}
}
