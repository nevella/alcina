/*
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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.ide.widget.CollapseEvent;
import cc.alcina.framework.gwt.client.ide.widget.CollapseEvent.CollapseHandler;
import cc.alcina.framework.gwt.client.ide.widget.CollapseEvent.HasCollapseHandlers;

/**
 * 
 * @author Nick Reddel
 */
@SuppressWarnings("deprecation")
public class DivStackPanel extends ComplexPanel {
	private static final String DEFAULT_STYLENAME = "gwt-StackPanel";

	private static final String DEFAULT_ITEM_STYLENAME = DEFAULT_STYLENAME
			+ "Item";

	private int visibleStack = -1;

	private Element body;

	Map<Integer, StackTextRow> rowTexts = new HashMap<Integer, DivStackPanel.StackTextRow>();

	/**
	 * Creates an empty stack panel.
	 */
	public DivStackPanel() {
		body = DOM.createDiv();
		setElement(body);
		DOM.sinkEvents(body, Event.ONCLICK);
		setStyleName(DEFAULT_STYLENAME);
	}

	/**
	 * Adds a new child with the given widget.
	 * 
	 * @param w
	 *            the widget to be added
	 */
	@Override
	public void add(Widget w) {
		insert(w, getWidgetCount());
	}

	/**
	 * Adds a new child with the given widget and header.
	 * 
	 * @param w
	 *            the widget to be added
	 * @param stackText
	 *            the header text associated with this widget
	 */
	public void add(Widget w, String stackText) {
		add(w, stackText, false);
	}

	/**
	 * Adds a new child with the given widget and header, optionally
	 * interpreting the header as HTML.
	 * 
	 * @param w
	 *            the widget to be added
	 * @param stackText
	 *            the header text associated with this widget
	 * @param asHTML
	 *            <code>true</code> to treat the specified text as HTML
	 */
	public void add(Widget w, String stackText, boolean asHTML) {
		add(w);
		setStackText(getWidgetCount() - 1, stackText, asHTML);
	}

	/**
	 * @return a header element
	 */
	Element createHeaderElem() {
		return DOM.createDiv();
	}

	private int findDividerIndex(Element elem) {
		while (elem != getElement()) {
			String expando = DOM.getElementPropertyOrAttribute(elem, "__index");
			if (expando != null) {
				// Make sure it belongs to me!
				String hashString = DOM.getElementPropertyOrAttribute(elem,
						"__owner");
				int ownerHash = hashString.length() > 0
						? Integer.parseInt(hashString)
						: -1;
				if (ownerHash == hashCode()) {
					// Yes, it's mine.
					return Integer.parseInt(expando);
				} else {
					// It must belong to some nested StackPanel.
					return -1;
				}
			}
			elem = DOM.getParent(elem);
		}
		return -1;
	}

	/**
	 * Get the element that holds the header text given the header element
	 * created by #createHeaderElement.
	 * 
	 * @param headerElem
	 *            the header element
	 * @return the element around the header text
	 */
	Element getHeaderTextElem(Element headerElem) {
		return headerElem;
	}

	/**
	 * Gets the currently selected child index.
	 * 
	 * @return selected child
	 */
	public int getSelectedIndex() {
		return visibleStack;
	}

	/**
	 * Inserts a widget before the specified index.
	 * 
	 * @param w
	 *            the widget to be inserted
	 * @param beforeIndex
	 *            the index before which it will be inserted
	 * @throws IndexOutOfBoundsException
	 *             if <code>beforeIndex</code> is out of range
	 */
	public void insert(Widget w, int beforeIndex) {
		Element tdh = createHeaderElem();
		Element tdb = DOM.createDiv();
		// DOM indices are 2x logical indices; 2 dom elements per stack item
		beforeIndex = adjustIndex(w, beforeIndex);
		int effectiveIndex = beforeIndex * 2;
		// this ordering puts the body below the header
		DOM.insertChild(getElement(), tdb, effectiveIndex);
		DOM.insertChild(getElement(), tdh, effectiveIndex);
		// header styling
		setStyleName(tdh, DEFAULT_ITEM_STYLENAME, true);
		DOM.setElementPropertyInt(tdh, "__owner", hashCode());
		DOM.setElementProperty(tdh, "height", "1px");
		// body styling
		setStyleName(tdb, DEFAULT_STYLENAME + "Content", true);
		DOM.setElementProperty(tdb, "height", "100%");
		DOM.setElementProperty(tdb, "vAlign", "top");
		// Now that the DOM is connected, call insert (this ensures that
		// onLoad() is
		// not fired until the child widget is attached to the DOM).
		super.insert(w, tdb, beforeIndex, false);
		// Update indices of all elements to the right.
		updateIndicesFrom(beforeIndex);
		// Correct visible stack for new location.
		if (visibleStack == -1) {
			showStack(0);
		} else {
			setStackVisible(beforeIndex, false);
			if (visibleStack >= beforeIndex) {
				++visibleStack;
			}
			// Reshow the stack to apply style names
			setStackVisible(visibleStack, true);
		}
	}

	public void moveChildrenTo(DivStackPanel other) {
		int i = 0;
		while (getWidgetCount() != 0) {
			StackTextRow rt = rowTexts.get(i++);
			other.add(getWidget(0), rt.text, rt.asHTML);
		}
	}

	@Override
	public void onBrowserEvent(Event event) {
		if (DOM.eventGetType(event) == Event.ONCLICK) {
			Element target = DOM.eventGetTarget(event);
			int index = findDividerIndex(target);
			if (index != -1) {
				showStack(index);
			}
			if (target.getTagName().equalsIgnoreCase("A")
					&& target.getAttribute("href").matches("#?")) {
				event.preventDefault();
			}
		}
	}

	/**
	 * <b>Affected Elements:</b>
	 * <ul>
	 * <li>-text# = The element around the header at the specified index.</li>
	 * <li>-text-wrapper# = The element around the header at the specified
	 * index.</li>
	 * <li>-content# = The element around the body at the specified index.</li>
	 * </ul>
	 * 
	 * @see UIObject#onEnsureDebugId(String)
	 */
	@Override
	protected void onEnsureDebugId(String baseID) {
		super.onEnsureDebugId(baseID);
	}

	@Override
	public boolean remove(int index) {
		return remove(getWidget(index), index);
	}

	@Override
	public boolean remove(Widget child) {
		return remove(child, getWidgetIndex(child));
	}

	private boolean remove(Widget child, int index) {
		// Make sure to call this before disconnecting the DOM.
		boolean removed = super.remove(child);
		if (removed) {
			// Calculate which internal table elements to remove.
			int rowIndex = 2 * index;
			Element tr = DOM.getChild(body, rowIndex);
			DOM.removeChild(body, tr);
			tr = DOM.getChild(body, rowIndex);
			DOM.removeChild(body, tr);
			// Correct visible stack for new location.
			if (visibleStack == index) {
				visibleStack = -1;
			} else if (visibleStack > index) {
				--visibleStack;
			}
			// Update indices of all elements to the right.
			updateIndicesFrom(index);
		}
		return removed;
	}

	private void setStackContentVisible(int index, boolean visible) {
		Element tr = DOM.getChild(body, (index * 2) + 1);
		UIObject.setVisible(tr, visible);
		getWidget(index).setVisible(visible);
	}

	/**
	 * Sets the text associated with a child by its index.
	 * 
	 * @param index
	 *            the index of the child whose text is to be set
	 * @param text
	 *            the text to be associated with it
	 */
	public void setStackText(int index, String text) {
		setStackText(index, text, false);
	}

	/**
	 * Sets the text associated with a child by its index.
	 * 
	 * @param index
	 *            the index of the child whose text is to be set
	 * @param text
	 *            the text to be associated with it
	 * @param asHTML
	 *            <code>true</code> to treat the specified text as HTML
	 */
	public void setStackText(int index, String text, boolean asHTML) {
		if (index >= getWidgetCount()) {
			return;
		}
		Element tdWrapper = DOM.getChild(body, index * 2);
		Element headerElem = tdWrapper;
		rowTexts.put(index, new StackTextRow(text, asHTML));
		if (asHTML) {
			DOM.setInnerHTML(getHeaderTextElem(headerElem), text);
		} else {
			DOM.setInnerText(getHeaderTextElem(headerElem), text);
		}
	}

	private void setStackVisible(int index, boolean visible) {
		// Get the first table row containing the widget's selector item.
		Element tr = DOM.getChild(body, (index * 2));
		if (tr == null) {
			return;
		}
		// Style the stack selector item.
		Element td = tr;
		setStyleName(td, DEFAULT_ITEM_STYLENAME + "-selected", visible);
		// Show/hide the contained widget.
		setStackContentVisible(index, visible);
		// Set the style of the next header
		Element trNext = DOM.getChild(body, ((index + 1) * 2));
		if (trNext != null) {
			setStyleName(trNext, DEFAULT_ITEM_STYLENAME + "-below-selected",
					visible);
		}
	}

	/**
	 * Shows the widget at the specified child index.
	 * 
	 * @param index
	 *            the index of the child to be shown
	 */
	public void showStack(int index) {
		if ((index >= getWidgetCount()) || (index < 0)
				|| (index == visibleStack)) {
			return;
		}
		int oldIndex = visibleStack;
		if (visibleStack >= 0) {
			setStackVisible(visibleStack, false);
		}
		visibleStack = index;
		setStackVisible(visibleStack, true);
	}

	private void updateIndicesFrom(int beforeIndex) {
		for (int i = beforeIndex, c = getWidgetCount(); i < c; ++i) {
			Element childTR = DOM.getChild(body, i * 2);
			Element childTD = childTR;
			DOM.setElementPropertyInt(childTD, "__index", i);
			// Update the special style on the first element
			if (beforeIndex == 0) {
				setStyleName(childTD, DEFAULT_ITEM_STYLENAME + "-first", true);
			} else {
				setStyleName(childTD, DEFAULT_ITEM_STYLENAME + "-first", false);
			}
		}
	}

	public static class CollapsableDivStackPanel extends DivStackPanel
			implements HasCollapseHandlers<CollapsableDivStackPanel> {
		private boolean inClick;

		@Override
		public HandlerRegistration addCollapseHandler(
				CollapseHandler<CollapsableDivStackPanel> handler) {
			return addHandler(handler, CollapseEvent.getType());
		}

		@Override
		public void onBrowserEvent(Event event) {
			if (DOM.eventGetType(event) == Event.ONCLICK) {
				inClick = true;
			}
			try {
				super.onBrowserEvent(event);
			} finally {
				inClick = false;
			}
		}

		@Override
		public void showStack(int index) {
			if (inClick && index != -1 && index == getSelectedIndex()) {
				// collapse
				fireEvent(new CollapseEvent(this, true));
			}
			super.showStack(index);
		}
	}

	static class StackTextRow {
		String text;

		boolean asHTML;

		public StackTextRow(String text, boolean asHTML) {
			this.text = text;
			this.asHTML = asHTML;
		}
	}
}
