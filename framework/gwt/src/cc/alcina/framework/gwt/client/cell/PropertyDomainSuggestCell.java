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

import java.util.function.Function;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ValueUpdater;
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

import cc.alcina.framework.gwt.client.gwittir.widget.BoundSuggestBox;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchSelector;

/**
 *
 */
public class PropertyDomainSuggestCell<T> extends AbstractEditableCell<T, T> {
	private static final int ESCAPE = 27;

	private int offsetX = 0;

	private int offsetY = -4;

	private Object lastKey;

	private Element lastParent;

	private int lastIndex;

	private int lastColumn;

	private T lastValue;

	private PopupPanel panel;

	private final SafeHtmlRenderer<String> renderer;

	private ValueUpdater<T> valueUpdater;

	private BoundSuggestBox suggestor;

	private Function<T, String> toStringMapper;

	public PropertyDomainSuggestCell(Function<T, String> toStringMapper,
			BoundSuggestBox suggestor) {
		super(CLICK, KEYDOWN);
		this.toStringMapper = toStringMapper;
		this.suggestor = suggestor;
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
		panel.add(suggestor);
		// Hide the panel and call valueUpdater.update when a value is selected
		suggestor.addPropertyChangeListener("value", event -> {
			// Remember the values before hiding the popup.
			Element cellParent = lastParent;
			T oldValue = lastValue;
			Object key = lastKey;
			int index = lastIndex;
			int column = lastColumn;
			panel.hide();
			// Update the cell and value updater.
			T value = (T) event.getNewValue();
			setViewData(key, value);
			setValue(new Context(index, column, key), cellParent, oldValue);
			if (valueUpdater != null) {
				valueUpdater.update(value);
			}
			lastFilterText = suggestor.getLastFilterText();
		});
	}

	String lastFilter = "";

	private String lastFilterText;

	@Override
	public boolean isEditing(Context context, Element parent, T value) {
		return lastKey != null && lastKey.equals(context.getKey());
	}

	@Override
	public void onBrowserEvent(Context context, Element parent, T value,
			NativeEvent event, ValueUpdater<T> valueUpdater) {
		super.onBrowserEvent(context, parent, value, event, valueUpdater);
		if (CLICK.equals(event.getType())) {
			onEnterKeyDown(context, parent, value, event, valueUpdater);
		}
	}

	@Override
	public void render(Context context, T value, SafeHtmlBuilder sb) {
		// Get the view data.
		Object key = context.getKey();
		T viewData = getViewData(key);
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
	protected void onEnterKeyDown(Context context, Element parent, T value,
			NativeEvent event, ValueUpdater<T> valueUpdater) {
		this.lastKey = context.getKey();
		this.lastParent = parent;
		this.lastValue = value;
		this.lastIndex = context.getIndex();
		this.lastColumn = context.getColumn();
		this.valueUpdater = valueUpdater;
		T viewData = getViewData(lastKey);
		suggestor.setValue(value);
		int tdWidth = parent.getParentElement().getOffsetWidth();
		panel.setPopupPositionAndShow(new PositionCallback() {
			public void setPosition(int offsetWidth, int offsetHeight) {
				panel.setWidth(tdWidth + "px");
				panel.setPopupPosition(lastParent.getAbsoluteLeft() + offsetX,
						lastParent.getAbsoluteTop() + offsetY);
				suggestor.setFilterText(lastFilterText);
			}
		});
	}
}
