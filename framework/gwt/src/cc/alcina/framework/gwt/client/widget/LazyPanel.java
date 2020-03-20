/*
 * Copyright 2008 Google Inc.
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
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Convenience class to help lazy loading. A LazyPanel is only actually rendered
 * when setVisible(true) or ensureWidget is called.
 * 
 * @param <T>
 *            widget type
 */
public abstract class LazyPanel<T extends Widget> extends SimplePanel {
	public static void
			addBeforeSelectionHandler(final CustomisableTabPanel panel) {
		panel.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
			public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
				Integer tabIndex = event.getItem();
				Widget widget = panel.getWidget(tabIndex);
				if (widget instanceof LazyPanel) {
					((LazyPanel) widget).ensureWidget();
				}
			}
		});
	}

	/**
	 * Adds a tab listener to ensure that any {@link LazyPanel} instances are
	 * loaded when a tab panel selects the lazy panel's tab.
	 * 
	 * @param panel
	 *            to add listener to
	 */
	public static void addBeforeSelectionHandler(final TabPanel panel) {
		panel.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
			public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
				Integer tabIndex = event.getItem();
				Widget widget = panel.getWidget(tabIndex);
				if (widget instanceof LazyPanel) {
					((LazyPanel) widget).ensureWidget();
				}
			}
		});
	}

	public LazyPanel() {
	}

	/**
	 * Create the widget contained within the {@link LazyPanel}.
	 * 
	 * @return the lazy widget
	 */
	public abstract T createWidget();

	/**
	 * Ensures the existence of the {@link LazyPanel} widget.
	 * 
	 * @return the widget
	 */
	public T ensureWidget() {
		T widget = (T) getWidget();
		if (widget == null) {
			widget = createWidget();
			setWidget(widget);
		}
		return widget;
	}

	/**
	 * Gets the panel's child widget. Will not create the widget, call
	 * <code>ensureWidget</code> to ensure that the widget exists.
	 * 
	 * @return the child widget, or <code>null</code> if none is present
	 */
	
	public T getWidget() {
		return (T) super.getWidget();
	}

	@Override
	/**
	 * Sets whether this object is visible. Creates the widget if necessary.
	 * 
	 * @param visible
	 *            <code>true</code> to show the object, <code>false</code> to
	 *            hide it
	 */
	public void setVisible(boolean visible) {
		if (visible) {
			ensureWidget();
		}
		super.setVisible(visible);
	}
}
