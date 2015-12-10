/*
 * Copyright 2010 Google Inc.
 *
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
package cc.alcina.framework.gwt.client.cell;

import static com.google.gwt.dom.client.BrowserEvents.*;

import java.util.Set;
import java.util.function.Function;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;

/**
 *
 */
public class PropertySelectorCell<T extends HasIdAndLocalId>
		extends AbstractEditableCell<Set<T>, Set<T>> {
	private static final int ESCAPE = 27;

	private int offsetX = 0;

	private int offsetY = 0;

	private Object lastKey;

	private Element lastParent;

	private int lastIndex;

	private int lastColumn;

	private Set<T> lastValue;

	private PopupPanel panel;

	private final SafeHtmlRenderer<String> renderer;

	private ValueUpdater<Set<T>> valueUpdater;

	private FlatSearchSelector selector;

	private Function<Set<T>, String> toStringMapper;

	public PropertySelectorCell(Class<T> selectionObjectClass,
			Function<Set<T>, String> toStringMapper,
			FlatSearchSelector selector) {
		super(CLICK, KEYDOWN);
		this.toStringMapper = toStringMapper;
		this.selector = selector;
		this.renderer = SimpleSafeHtmlRenderer.getInstance();
		this.panel = new PopupPanel(true, true) {
			@Override
			protected void onPreviewNativeEvent(NativePreviewEvent event) {
				if (Event.ONKEYUP == event.getTypeInt()) {
					if (event.getNativeEvent().getKeyCode() == ESCAPE) {
						// Dismiss when escape is pressed
						panel.hide();
					}
				}
			}
		};
		panel.addStyleName("property-selector");
		panel.addCloseHandler(new CloseHandler<PopupPanel>() {
			public void onClose(CloseEvent<PopupPanel> event) {
				lastKey = null;
				lastValue = null;
				lastIndex = -1;
				lastColumn = -1;
				if (lastParent != null && !event.isAutoClosed()) {
					// Refocus on the containing cell after the user selects a
					// value, but
					// not if the popup is auto closed.
					lastParent.focus();
				}
				lastParent = null;
			}
		});
		panel.add(selector);
		// Hide the panel and call valueUpdater.update when a value is selected
		selector.addPropertyChangeListener("value", event -> {
			// Remember the values before hiding the popup.
			Element cellParent = lastParent;
			Set<T> oldValue = lastValue;
			Object key = lastKey;
			int index = lastIndex;
			int column = lastColumn;
			panel.hide();
			// Update the cell and value updater.
			Set<T> value = (Set<T>) event.getNewValue();
			setViewData(key, value);
			setValue(new Context(index, column, key), cellParent, oldValue);
			if (valueUpdater != null) {
				valueUpdater.update(value);
			}
		});
	}

	@Override
	public boolean isEditing(Context context, Element parent, Set<T> value) {
		return lastKey != null && lastKey.equals(context.getKey());
	}

	@Override
	public void onBrowserEvent(Context context, Element parent, Set<T> value,
			NativeEvent event, ValueUpdater<Set<T>> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			onEnterKeyDown(context, parent, value, event, valueUpdater);
		}
	}

	@Override
	public void render(Context context, Set<T> value, SafeHtmlBuilder sb) {
		// Get the view data.
		Object key = context.getKey();
		Set<T> viewData = getViewData(key);
		if (viewData != null && viewData.equals(value)) {
			clearViewData(key);
			viewData = null;
		}
		String s = null;
		if (viewData != null) {
			s = toStringMapper.apply(viewData);
		} else if (value != null) {
			s = toStringMapper.apply(value);
		}
		if (s != null) {
			sb.append(renderer.render(s));
		}
	}

	@Override
	protected void onEnterKeyDown(Context context, Element parent, Set<T> value,
			NativeEvent event, ValueUpdater<Set<T>> valueUpdater) {
		this.lastKey = context.getKey();
		this.lastParent = parent;
		this.lastValue = value;
		this.lastIndex = context.getIndex();
		this.lastColumn = context.getColumn();
		this.valueUpdater = valueUpdater;
		Set<T> viewData = getViewData(lastKey);
		selector.setValue(value);
		panel.setPopupPositionAndShow(new PositionCallback() {
			public void setPosition(int offsetWidth, int offsetHeight) {
				panel.setPopupPosition(lastParent.getAbsoluteLeft() + offsetX,
						lastParent.getAbsoluteTop() + offsetY);
				selector.clearFilter();
				Scheduler.get().scheduleDeferred(() -> selector.showOptions());
			}
		});
	}
}
