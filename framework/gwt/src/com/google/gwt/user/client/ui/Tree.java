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
package com.google.gwt.user.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype.ImagePrototypeElement;

/**
 * A standard hierarchical tree widget. The tree contains a hierarchy of
 * {@link com.google.gwt.user.client.ui.TreeItem TreeItems} that the user can
 * open, close, and select.
 * <p>
 * <img class='gallery' src='doc-files/Tree.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Tree</dt>
 * <dd>the tree itself</dd>
 * <dt>.gwt-Tree .gwt-TreeItem</dt>
 * <dd>a tree item</dd>
 * <dt>.gwt-Tree .gwt-TreeItem-selected</dt>
 * <dd>a selected tree item</dd>
 * </dl>
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TreeExample}
 * </p>
 */
@SuppressWarnings("deprecation")
public class Tree extends Widget implements HasTreeItems.ForIsWidget,
		HasWidgets.ForIsWidget, SourcesTreeEvents, HasFocus, HasAnimation,
		HasAllKeyHandlers, HasAllFocusHandlers, HasSelectionHandlers<TreeItem>,
		HasOpenHandlers<TreeItem>, HasCloseHandlers<TreeItem>,
		SourcesMouseEvents, HasAllMouseHandlers {
	/*
	 * For compatibility with UiBinder interface HasTreeItems should be declared
	 * before HasWidgets, so that corresponding parser will run first and add
	 * TreeItem children as items, not as widgets.
	 */
	private static final int OTHER_KEY_DOWN = 63233;

	private static final int OTHER_KEY_LEFT = 63234;

	private static final int OTHER_KEY_RIGHT = 63235;

	private static final int OTHER_KEY_UP = 63232;

	/*
	 * for testing
	 */
	public static boolean handleClicksAsMousedown;

	private static boolean isArrowKey(int code) {
		switch (code) {
		case OTHER_KEY_DOWN:
		case OTHER_KEY_RIGHT:
		case OTHER_KEY_UP:
		case OTHER_KEY_LEFT:
		case KeyCodes.KEY_DOWN:
		case KeyCodes.KEY_RIGHT:
		case KeyCodes.KEY_UP:
		case KeyCodes.KEY_LEFT:
			return true;
		default:
			return false;
		}
	}

	static native boolean shouldTreeDelegateFocusToElement(Element elem) /*-{
    var name = elem.nodeName;
    return ((name == "SELECT") || (name == "INPUT") || (name == "TEXTAREA")
        || (name == "OPTION") || (name == "BUTTON") || (name == "LABEL"));
	}-*/;

	/**
	 * Normalized key codes. Also switches KEY_RIGHT and KEY_LEFT in RTL
	 * languages.
	 */
	private static int standardizeKeycode(int code) {
		switch (code) {
		case OTHER_KEY_DOWN:
			code = KeyCodes.KEY_DOWN;
			break;
		case OTHER_KEY_RIGHT:
			code = KeyCodes.KEY_RIGHT;
			break;
		case OTHER_KEY_UP:
			code = KeyCodes.KEY_UP;
			break;
		case OTHER_KEY_LEFT:
			code = KeyCodes.KEY_LEFT;
			break;
		}
		if (LocaleInfo.getCurrentLocale().isRTL()) {
			if (code == KeyCodes.KEY_RIGHT) {
				code = KeyCodes.KEY_LEFT;
			} else if (code == KeyCodes.KEY_LEFT) {
				code = KeyCodes.KEY_RIGHT;
			}
		}
		return code;
	}

	/**
	 * Map of TreeItem.widget -> TreeItem.
	 */
	private final Map<Widget, TreeItem> childWidgets = new HashMap<Widget, TreeItem>();

	private TreeItem curSelection;

	private Element focusable;

	private ImageAdapter images;

	private String indentValue;

	private boolean isAnimationEnabled = false;

	private boolean lastWasKeyDown;

	private TreeItem root;

	private boolean useLeafImages;

	private AbstractImagePrototypeCache prototypeCache = new AbstractImagePrototypeCache();

	/**
	 * Constructs an empty tree.
	 */
	public Tree() {
		init(new ImageAdapter(), false);
	}

	/**
	 * Constructs a tree that uses the specified ClientBundle for images.
	 *
	 * @param resources
	 *            a bundle that provides tree specific images
	 */
	public Tree(Resources resources) {
		init(new ImageAdapter(resources), false);
	}

	/**
	 * Constructs a tree that uses the specified ClientBundle for images. If
	 * this tree does not use leaf images, the width of the Resources's leaf
	 * image will control the leaf indent.
	 *
	 * @param resources
	 *            a bundle that provides tree specific images
	 * @param useLeafImages
	 *            use leaf images from bundle
	 */
	public Tree(Resources resources, boolean useLeafImages) {
		init(new ImageAdapter(resources), useLeafImages);
	}

	/**
	 * Constructs a tree that uses the specified image bundle for images.
	 *
	 * @param images
	 *            a bundle that provides tree specific images
	 * @deprecated replaced by {@link #Tree(Resources)}
	 */
	@Deprecated
	public Tree(TreeImages images) {
		init(new ImageAdapter(images), false);
	}

	/**
	 * Constructs a tree that uses the specified image bundle for images. If
	 * this tree does not use leaf images, the width of the TreeImage's leaf
	 * image will control the leaf indent.
	 *
	 * @param images
	 *            a bundle that provides tree specific images
	 * @param useLeafImages
	 *            use leaf images from bundle
	 * @deprecated replaced by {@link #Tree(Resources, boolean)}
	 */
	@Deprecated
	public Tree(TreeImages images, boolean useLeafImages) {
		init(new ImageAdapter(images), useLeafImages);
	}

	/**
	 * Overloaded version for IsWidget.
	 *
	 * @see #add(Widget)
	 */
	@Override
	public void add(IsWidget w) {
		this.add(asWidgetOrNull(w));
	}

	/**
	 * Adds the widget as a root tree item.
	 *
	 * @see com.google.gwt.user.client.ui.HasWidgets#add(com.google.gwt.user.client.ui.Widget)
	 * @param widget
	 *            widget to add.
	 */
	@Override
	public void add(Widget widget) {
		addItem(widget);
	}

	@Override
	public HandlerRegistration addBlurHandler(BlurHandler handler) {
		return addDomHandler(handler, BlurEvent.getType());
	}

	@Override
	public HandlerRegistration addCloseHandler(CloseHandler<TreeItem> handler) {
		return addHandler(handler, CloseEvent.getType());
	}

	@Override
	public HandlerRegistration addFocusHandler(FocusHandler handler) {
		return addDomHandler(handler, FocusEvent.getType());
	}

	/**
	 * @deprecated Use {@link #addFocusHandler} instead
	 */
	@Override
	@Deprecated
	public void addFocusListener(FocusListener listener) {
		ListenerWrapper.WrappedFocusListener.add(this, listener);
	}

	/**
	 * Adds an item to the root level of this tree.
	 *
	 * @param isItem
	 *            the wrapper of item to be added
	 */
	@Override
	public void addItem(IsTreeItem isItem) {
		root.addItem(isItem);
	}

	/**
	 * Overloaded version for IsWidget.
	 *
	 * @see #addItem(Widget)
	 */
	@Override
	public TreeItem addItem(IsWidget w) {
		return this.addItem(asWidgetOrNull(w));
	}

	/**
	 * Adds a simple tree item containing the specified html.
	 *
	 * @param itemHtml
	 *            the html of the item to be added
	 * @return the item that was added
	 */
	@Override
	public TreeItem addItem(SafeHtml itemHtml) {
		return root.addItem(itemHtml);
	}

	/**
	 * Adds a simple tree item containing the specified html.
	 *
	 * @param itemHtml
	 *            the text of the item to be added
	 * @return the item that was added
	 */
	public TreeItem addItem(String itemHtml) {
		return root.addItem(itemHtml);
	}

	/**
	 * Adds an item to the root level of this tree.
	 *
	 * @param item
	 *            the item to be added
	 */
	@Override
	public void addItem(TreeItem item) {
		root.addItem(item);
	}

	/**
	 * Adds a new tree item containing the specified widget.
	 *
	 * @param widget
	 *            the widget to be added
	 * @return the new item
	 */
	@Override
	public TreeItem addItem(Widget widget) {
		return root.addItem(widget);
	}

	/**
	 * @deprecated Use {@link #addKeyDownHandler}, {@link #addKeyUpHandler} and
	 *             {@link #addKeyPressHandler} instead
	 */
	@Override
	@Deprecated
	public void addKeyboardListener(KeyboardListener listener) {
		ListenerWrapper.WrappedKeyboardListener.add(this, listener);
	}

	@Override
	public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
		return addDomHandler(handler, KeyDownEvent.getType());
	}

	@Override
	public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
		return addDomHandler(handler, KeyPressEvent.getType());
	}

	@Override
	public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
		return addDomHandler(handler, KeyUpEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
		return addHandler(handler, MouseDownEvent.getType());
	}

	/**
	 * @deprecated Use {@link #addMouseOverHandler} {@link #addMouseMoveHandler}
	 *             , {@link #addMouseDownHandler}, {@link #addMouseUpHandler}
	 *             and {@link #addMouseOutHandler} instead
	 */
	@Override
	@Deprecated
	public void addMouseListener(MouseListener listener) {
		ListenerWrapper.WrappedMouseListener.add(this, listener);
	}

	@Override
	public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
		return addDomHandler(handler, MouseMoveEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
		return addDomHandler(handler, MouseOutEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
		return addDomHandler(handler, MouseOverEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
		return addDomHandler(handler, MouseUpEvent.getType());
	}

	@Override
	public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
		return addDomHandler(handler, MouseWheelEvent.getType());
	}

	@Override
	public final HandlerRegistration
			addOpenHandler(OpenHandler<TreeItem> handler) {
		return addHandler(handler, OpenEvent.getType());
	}

	@Override
	public HandlerRegistration
			addSelectionHandler(SelectionHandler<TreeItem> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	/**
	 * Adds a simple tree item containing the specified text.
	 *
	 * @param itemText
	 *            the text of the item to be added
	 * @return the item that was added
	 */
	@Override
	public TreeItem addTextItem(String itemText) {
		return root.addTextItem(itemText);
	}

	/**
	 * @deprecated Use {@link #addSelectionHandler}, {@link #addOpenHandler},
	 *             and {@link #addCloseHandler} instead
	 */
	@Override
	@Deprecated
	public void addTreeListener(TreeListener listener) {
		ListenerWrapper.WrappedTreeListener.add(this, listener);
	}

	void adopt(Widget widget, TreeItem treeItem) {
		assert (!childWidgets.containsKey(widget));
		childWidgets.put(widget, treeItem);
		widget.setParent(this);
	}

	/**
	 * Clears all tree items from the current tree.
	 */
	@Override
	public void clear() {
		int size = root.getChildCount();
		for (int i = size - 1; i >= 0; i--) {
			root.getChild(i).remove();
		}
	}

	/**
	 * Collects parents going up the element tree, terminated at the tree root.
	 */
	private void collectElementChain(ArrayList<Element> chain, Element hRoot,
			Element hElem) {
		if ((hElem == null) || (hElem == hRoot)) {
			return;
		}
		collectElementChain(chain, hRoot, DOM.getParent(hElem));
		chain.add(hElem);
	}

	@Override
	protected void doAttachChildren() {
		try {
			AttachDetachException.tryCommand(this,
					AttachDetachException.attachCommand);
		} finally {
			DOM.setEventListener(focusable, this);
		}
	}

	@Override
	protected void doDetachChildren() {
		try {
			AttachDetachException.tryCommand(this,
					AttachDetachException.detachCommand);
		} finally {
			DOM.setEventListener(focusable, null);
		}
	}

	private boolean elementClicked(Element hElem) {
		ArrayList<Element> chain = new ArrayList<Element>();
		collectElementChain(chain, getElement(), hElem);
		TreeItem item = findItemByChain(chain, 0, root);
		if (item != null && item != root) {
			if (item.getChildCount() > 0
					&& DOM.isOrHasChild(item.getImageElement(), hElem)) {
				item.setState(!item.getState(), true);
				return true;
			} else if (DOM.isOrHasChild(item.getElement(), hElem)) {
				if (isToggleSelectionOnLabelClick() && item.isSelected()) {
					Scheduler.get().scheduleFinally(
							() -> item.setState(!item.getState(), true));
				}
				onSelection(item, true,
						!shouldTreeDelegateFocusToElement(hElem));
				return true;
			}
		}
		return false;
	}

	/**
	 * Ensures that the currently-selected item is visible, opening its parents
	 * and scrolling the tree as necessary.
	 */
	public void ensureSelectedItemVisible() {
		if (curSelection == null) {
			return;
		}
		TreeItem parent = curSelection.getParentItem();
		while (parent != null) {
			parent.setState(true);
			parent = parent.getParentItem();
		}
	}

	private TreeItem findDeepestOpenChild(TreeItem item) {
		if (!item.getState()) {
			return item;
		}
		return findDeepestOpenChild(item.getChild(item.getChildCount() - 1));
	}

	private TreeItem findItemByChain(ArrayList<Element> chain, int idx,
			TreeItem root) {
		if (idx == chain.size()) {
			return root;
		}
		Element hCurElem = chain.get(idx);
		for (int i = 0, n = root.getChildCount(); i < n; ++i) {
			TreeItem child = root.getChild(i);
			if (!child.isUnrendered() && child.getElement() == hCurElem) {
				TreeItem retItem = findItemByChain(chain, idx + 1, child);
				if (retItem == null) {
					return child;
				}
				return retItem;
			}
		}
		return findItemByChain(chain, idx + 1, root);
	}

	void fireStateChanged(TreeItem item, boolean open) {
		if (open) {
			OpenEvent.fire(this, item);
		} else {
			CloseEvent.fire(this, item);
		}
	}

	/*
	 * This method exists solely to support unit tests.
	 */
	Map<Widget, TreeItem> getChildWidgets() {
		return childWidgets;
	}

	ImageAdapter getImages() {
		return images;
	}

	/**
	 * Gets the top-level tree item at the specified index.
	 *
	 * @param index
	 *            the index to be retrieved
	 * @return the item at that index
	 */
	public TreeItem getItem(int index) {
		return root.getChild(index);
	}

	/**
	 * Gets the number of items contained at the root of this tree.
	 *
	 * @return this tree's item count
	 */
	public int getItemCount() {
		return root.getChildCount();
	}

	/**
	 * Gets the currently selected item.
	 *
	 * @return the selected item
	 */
	public TreeItem getSelectedItem() {
		return curSelection;
	}

	@Override
	public int getTabIndex() {
		return FocusPanel.impl.getTabIndex(focusable);
	}

	/**
	 * Get the top parent above this {@link TreeItem} that is in closed state.
	 * In other words, get the parent that is guaranteed to be visible.
	 *
	 * @param item
	 * @return the closed parent, or null if all parents are opened
	 */
	private TreeItem getTopClosedParent(TreeItem item) {
		TreeItem topClosedParent = null;
		TreeItem parent = item.getParentItem();
		while (parent != null && parent != root) {
			if (!parent.getState()) {
				topClosedParent = parent;
			}
			parent = parent.getParentItem();
		}
		return topClosedParent;
	}

	private void init(ImageAdapter images, boolean useLeafImages) {
		setImages(images, useLeafImages);
		setElement(DOM.createDiv());
		DOM.setStyleAttribute(getElement(), "position", "relative");
		// Fix rendering problem with relatively-positioned elements and their
		// children by
		// forcing the element that is positioned relatively to 'have layout'
		DOM.setStyleAttribute(getElement(), "zoom", "1");
		focusable = FocusPanel.impl.createFocusable().cast();
		DOM.setStyleAttribute(focusable, "fontSize", "0");
		DOM.setStyleAttribute(focusable, "position", "absolute");
		// Hide focus outline in Mozilla/Webkit/Opera
		DOM.setStyleAttribute(focusable, "outline", "0px");
		// Hide focus outline in IE 6/7
		DOM.setElementAttribute(focusable, "hideFocus", "true");
		DOM.setIntStyleAttribute(focusable, "zIndex", -1);
		DOM.appendChild(getElement(), focusable);
		sinkEvents(Event.ONMOUSEDOWN | Event.ONCLICK | Event.KEYEVENTS);
		DOM.sinkEvents(focusable, Event.FOCUSEVENTS);
		// The 'root' item is invisible and serves only as a container
		// for all top-level items.
		root = new TreeItem(true);
		root.setTree(this);
		setStyleName("gwt-Tree");
		// Add a11y role "tree"
		Accessibility.setRole(getElement(), Accessibility.ROLE_TREE);
		Accessibility.setRole(focusable, Accessibility.ROLE_TREEITEM);
	}

	/**
	 * Inserts a child tree item at the specified index containing the specified
	 * html.
	 *
	 * @param beforeIndex
	 *            the index where the item will be inserted
	 * @param itemHtml
	 *            the html of the item to be added
	 * @return the item that was added
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public TreeItem insertItem(int beforeIndex, SafeHtml itemHtml) {
		return root.insertItem(beforeIndex, itemHtml);
	}

	/**
	 * Inserts a child tree item at the specified index containing the specified
	 * text.
	 *
	 * @param beforeIndex
	 *            the index where the item will be inserted
	 * @param itemText
	 *            the text to be added
	 * @return the item that was added
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public TreeItem insertItem(int beforeIndex, String itemText) {
		return root.insertItem(beforeIndex, itemText);
	}

	/**
	 * Inserts an item into the root level of this tree.
	 *
	 * @param beforeIndex
	 *            the index where the item will be inserted
	 * @param item
	 *            the item to be added
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public void insertItem(int beforeIndex, TreeItem item) {
		root.insertItem(beforeIndex, item);
	}

	/**
	 * Inserts a child tree item at the specified index containing the specified
	 * widget.
	 *
	 * @param beforeIndex
	 *            the index where the item will be inserted
	 * @param widget
	 *            the widget to be added
	 * @return the item that was added
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of range
	 */
	public TreeItem insertItem(int beforeIndex, Widget widget) {
		return root.insertItem(beforeIndex, widget);
	}

	@Override
	public boolean isAnimationEnabled() {
		return isAnimationEnabled;
	}

	/**
	 * Indicates if keyboard navigation is enabled for the Tree and for a given
	 * TreeItem. Subclasses of Tree can override this function to selectively
	 * enable or disable keyboard navigation.
	 *
	 * @param currentItem
	 *            the currently selected TreeItem
	 * @return <code>true</code> if the Tree will response to arrow keys by
	 *         changing the currently selected item
	 */
	protected boolean isKeyboardNavigationEnabled(TreeItem currentItem) {
		return true;
	}

	protected boolean isToggleSelectionOnLabelClick() {
		return false;
	}

	@Override
	public Iterator<Widget> iterator() {
		final Widget[] widgets = new Widget[childWidgets.size()];
		childWidgets.keySet().toArray(widgets);
		return WidgetIterators.createWidgetIterator(this, widgets);
	}

	private void keyboardNavigation(Event event) {
		// Handle keyboard events if keyboard navigation is enabled
		if (isKeyboardNavigationEnabled(curSelection)) {
			int code = DOM.eventGetKeyCode(event);
			switch (standardizeKeycode(code)) {
			case KeyCodes.KEY_UP: {
				moveSelectionUp(curSelection);
				break;
			}
			case KeyCodes.KEY_DOWN: {
				moveSelectionDown(curSelection, true);
				break;
			}
			case KeyCodes.KEY_LEFT: {
				maybeCollapseTreeItem();
				break;
			}
			case KeyCodes.KEY_RIGHT: {
				maybeExpandTreeItem();
				break;
			}
			default: {
				return;
			}
			}
		}
	}

	private void maybeCollapseTreeItem() {
		TreeItem topClosedParent = getTopClosedParent(curSelection);
		if (topClosedParent != null) {
			// Select the first visible parent if curSelection is hidden
			setSelectedItem(topClosedParent);
		} else if (curSelection.getState()) {
			curSelection.setState(false);
		} else {
			TreeItem parent = curSelection.getParentItem();
			if (parent != null) {
				setSelectedItem(parent);
			}
		}
	}

	private void maybeExpandTreeItem() {
		TreeItem topClosedParent = getTopClosedParent(curSelection);
		if (topClosedParent != null) {
			// Select the first visible parent if curSelection is hidden
			setSelectedItem(topClosedParent);
		} else if (!curSelection.getState()) {
			curSelection.setState(true);
		} else if (curSelection.getChildCount() > 0) {
			setSelectedItem(curSelection.getChild(0));
		}
	}

	void maybeUpdateSelection(TreeItem itemThatChangedState,
			boolean isItemOpening) {
		/**
		 * If we just closed the item, let's check to see if this item is the
		 * parent of the currently selected item. If so, we should make this
		 * item the currently selected selected item.
		 */
		if (!isItemOpening) {
			TreeItem tempItem = curSelection;
			while (tempItem != null) {
				if (tempItem == itemThatChangedState) {
					setSelectedItem(itemThatChangedState);
					return;
				}
				tempItem = tempItem.getParentItem();
			}
		}
	}

	/**
	 * Move the tree focus to the specified selected item.
	 */
	private void moveFocus() {
		Focusable focusableWidget = curSelection.getFocusable();
		if (focusableWidget != null) {
			focusableWidget.setFocus(true);
			DOM.scrollIntoView(((Widget) focusableWidget).getElement());
		} else {
			// Get the location and size of the given item's content element
			// relative
			// to the tree.
			Element selectedElem = curSelection.getContentElem();
			int containerLeft = getAbsoluteLeft();
			int containerTop = getAbsoluteTop();
			int left = selectedElem.getAbsoluteLeft() - containerLeft;
			int top = selectedElem.getAbsoluteTop() - containerTop;
			int width = selectedElem.getOffsetWidth();
			int height = selectedElem.getOffsetHeight();
			// If the item is not visible, quite here
			if (width == 0 || height == 0) {
				DOM.setIntStyleAttribute(focusable, "left", 0);
				DOM.setIntStyleAttribute(focusable, "top", 0);
				return;
			}
			// Set the focusable element's position and size to exactly underlap
			// the
			// item's content element.
			DOM.setStyleAttribute(focusable, "left", left + "px");
			DOM.setStyleAttribute(focusable, "top", top + "px");
			DOM.setStyleAttribute(focusable, "width", width + "px");
			DOM.setStyleAttribute(focusable, "height", height + "px");
			// Scroll it into view.
			DOM.scrollIntoView(focusable);
			// Update ARIA attributes to reflect the information from the
			// newly-selected item.
			updateAriaAttributes();
			// Ensure Focus is set, as focus may have been previously delegated
			// by
			// tree.
			setFocus(true);
		}
	}

	/**
	 * Moves to the next item, going into children as if dig is enabled.
	 */
	private void moveSelectionDown(TreeItem sel, boolean dig) {
		if (sel == root) {
			return;
		}
		// Find a parent that is visible
		TreeItem topClosedParent = getTopClosedParent(sel);
		if (topClosedParent != null) {
			moveSelectionDown(topClosedParent, false);
			return;
		}
		TreeItem parent = sel.getParentItem();
		if (parent == null) {
			parent = root;
		}
		int idx = parent.getChildIndex(sel);
		if (!dig || !sel.getState()) {
			if (idx < parent.getChildCount() - 1) {
				onSelection(parent.getChild(idx + 1), true, true);
			} else {
				moveSelectionDown(parent, false);
			}
		} else if (sel.getChildCount() > 0) {
			onSelection(sel.getChild(0), true, true);
		}
	}

	/**
	 * Moves the selected item up one.
	 */
	private void moveSelectionUp(TreeItem sel) {
		// Find a parent that is visible
		TreeItem topClosedParent = getTopClosedParent(sel);
		if (topClosedParent != null) {
			onSelection(topClosedParent, true, true);
			return;
		}
		TreeItem parent = sel.getParentItem();
		if (parent == null) {
			parent = root;
		}
		int idx = parent.getChildIndex(sel);
		if (idx > 0) {
			TreeItem sibling = parent.getChild(idx - 1);
			onSelection(findDeepestOpenChild(sibling), true, true);
		} else {
			onSelection(parent, true, true);
		}
	}

	@Override
	protected void onAttach() {
		root.updateStateRecursive();
		super.onAttach();
	}

	@Override
	@SuppressWarnings("fallthrough")
	public void onBrowserEvent(Event event) {
		int eventType = DOM.eventGetType(event);
		switch (eventType) {
		case Event.ONKEYDOWN: {
			// If nothing's selected, select the first item.
			if (curSelection == null) {
				if (root.getChildCount() > 0) {
					onSelection(root.getChild(0), true, true);
				}
				super.onBrowserEvent(event);
				return;
			}
		}
		// Intentional fallthrough.
		case Event.ONKEYPRESS:
		case Event.ONKEYUP:
			// Issue 1890: Do not block history navigation via alt+left/right
			if (DOM.eventGetAltKey(event) || DOM.eventGetMetaKey(event)) {
				super.onBrowserEvent(event);
				return;
			}
			break;
		}
		switch (eventType) {
		case Event.ONCLICK: {
			Element e = DOM.eventGetTarget(event);
			if (shouldTreeDelegateFocusToElement(e)) {
				// The click event should have given focus to this element
				// already.
				// Avoid moving focus back up to the tree (so that focusable
				// widgets
				// attached to TreeItems can receive keyboard events).
			} else if ((curSelection != null)
					&& curSelection.getContentElem().isOrHasChild(e)) {
				setFocus(true);
			}
			if (handleClicksAsMousedown) {
				elementClicked(DOM.eventGetTarget(event));
			}
			break;
		}
		case Event.ONMOUSEDOWN: {
			// Currently, the way we're using image bundles causes extraneous
			// events
			// to be sunk on individual items' open/close images. This leads to
			// an
			// extra event reaching the Tree, which we will ignore here.
			// Also, ignore middle and right clicks here.
			if ((DOM.eventGetCurrentTarget(event) == getElement())
					&& (event.getButton() == Event.BUTTON_LEFT)) {
				elementClicked(DOM.eventGetTarget(event));
			}
			break;
		}
		case Event.ONKEYDOWN: {
			keyboardNavigation(event);
			lastWasKeyDown = true;
			break;
		}
		case Event.ONKEYPRESS: {
			if (!lastWasKeyDown) {
				keyboardNavigation(event);
			}
			lastWasKeyDown = false;
			break;
		}
		case Event.ONKEYUP: {
			if (DOM.eventGetKeyCode(event) == KeyCodes.KEY_TAB) {
				ArrayList<Element> chain = new ArrayList<Element>();
				collectElementChain(chain, getElement(),
						DOM.eventGetTarget(event));
				TreeItem item = findItemByChain(chain, 0, root);
				if (item != getSelectedItem()) {
					setSelectedItem(item, true);
				}
			}
			lastWasKeyDown = false;
			break;
		}
		}
		switch (eventType) {
		case Event.ONKEYDOWN:
		case Event.ONKEYUP: {
			if (isArrowKey(DOM.eventGetKeyCode(event))) {
				DOM.eventCancelBubble(event, true);
				DOM.eventPreventDefault(event);
				return;
			}
		}
		}
		// We must call super for all handlers.
		super.onBrowserEvent(event);
	}

	/**
	 * <b>Affected Elements:</b>
	 * <ul>
	 * <li>-root = The root {@link TreeItem}.</li>
	 * </ul>
	 *
	 * @see UIObject#onEnsureDebugId(String)
	 */
	@Override
	protected void onEnsureDebugId(String baseID) {
		super.onEnsureDebugId(baseID);
		root.ensureDebugId(baseID + "-root");
	}

	@Override
	protected void onLoad() {
	}

	private void onSelection(TreeItem item, boolean fireEvents,
			boolean moveFocus) {
		// 'root' isn't a real item, so don't let it be selected
		// (some cases in the keyboard handler will try to do this)
		if (item == root) {
			return;
		}
		if (curSelection != null) {
			curSelection.setSelected(false);
		}
		curSelection = item;
		if (curSelection != null) {
			while ((item = item.getParentItem()) != root && item != null) {
				if (!item.getState()) {
					item.setState(true);
				}
			}
			if (moveFocus) {
				moveFocus();
			}
			// Select the item and fire the selection event.
			curSelection.setSelected(true);
			if (fireEvents) {
				SelectionEvent.fire(this, curSelection);
			}
		}
	}

	void orphan(Widget widget) {
		// Validation should already be done.
		assert (widget.getParent() == this);
		// Orphan.
		try {
			widget.setParent(null);
		} finally {
			// Logical detach.
			childWidgets.remove(widget);
		}
	}

	/**
	 * Overloaded version for IsWidget.
	 *
	 * @see #remove(Widget)
	 */
	@Override
	public boolean remove(IsWidget w) {
		return this.remove(w.asWidget());
	}

	@Override
	public boolean remove(Widget w) {
		// Validate.
		TreeItem item = childWidgets.get(w);
		if (item == null) {
			return false;
		}
		// Delegate to TreeItem.setWidget, which performs correct removal.
		item.setWidget(null);
		return true;
	}

	/**
	 * @deprecated Use the {@link HandlerRegistration#removeHandler} method on
	 *             the object returned by {@link #addFocusHandler} instead
	 */
	@Override
	@Deprecated
	public void removeFocusListener(FocusListener listener) {
		ListenerWrapper.WrappedFocusListener.remove(this, listener);
	}

	/**
	 * Removes an item from the root level of this tree.
	 *
	 * @param isItem
	 *            the wrapper of item to be removed
	 */
	@Override
	public void removeItem(IsTreeItem isItem) {
		if (isItem != null) {
			TreeItem item = isItem.asTreeItem();
			removeItem(item);
		}
	}

	/**
	 * Removes an item from the root level of this tree.
	 *
	 * @param item
	 *            the item to be removed
	 */
	@Override
	public void removeItem(TreeItem item) {
		root.removeItem(item);
	}

	/**
	 * Removes all items from the root level of this tree.
	 */
	@Override
	public void removeItems() {
		while (getItemCount() > 0) {
			removeItem(getItem(0));
		}
	}

	/**
	 * @deprecated Use the {@link HandlerRegistration#removeHandler} method on
	 *             the object returned by an add*Handler method instead
	 */
	@Override
	@Deprecated
	public void removeKeyboardListener(KeyboardListener listener) {
		ListenerWrapper.WrappedKeyboardListener.remove(this, listener);
	}

	/**
	 * @deprecated Use the {@link HandlerRegistration#removeHandler} method on
	 *             the object returned by an add*Handler method instead
	 */
	@Override
	@Deprecated
	public void removeMouseListener(MouseListener listener) {
		ListenerWrapper.WrappedMouseListener.remove(this, listener);
	}

	/**
	 * @deprecated Use the {@link HandlerRegistration#removeHandler} method on
	 *             the object returned by an add*Handler method instead
	 */
	@Override
	@Deprecated
	public void removeTreeListener(TreeListener listener) {
		ListenerWrapper.WrappedTreeListener.remove(this, listener);
	}

	@Override
	public void setAccessKey(char key) {
		FocusPanel.impl.setAccessKey(focusable, key);
	}

	@Override
	public void setAnimationEnabled(boolean enable) {
		isAnimationEnabled = enable;
	}

	@Override
	public void setFocus(boolean focus) {
		if (focus) {
			FocusPanel.impl.focus(focusable);
		} else {
			FocusPanel.impl.blur(focusable);
		}
	}

	private void setImages(ImageAdapter images, boolean useLeafImages) {
		this.images = images;
		this.useLeafImages = useLeafImages;
		if (!useLeafImages) {
			Image image = images.treeLeaf().createImage();
			DOM.setStyleAttribute(image.getElement(), "visibility", "hidden");
			RootPanel.get().add(image);
			int size = image.getWidth() + TreeItem.IMAGE_PAD;
			image.removeFromParent();
			indentValue = (size) + "px";
		}
	}

	/**
	 * Selects a specified item.
	 *
	 * @param item
	 *            the item to be selected, or <code>null</code> to deselect all
	 *            items
	 */
	public void setSelectedItem(TreeItem item) {
		setSelectedItem(item, true);
	}

	/**
	 * Selects a specified item.
	 *
	 * @param item
	 *            the item to be selected, or <code>null</code> to deselect all
	 *            items
	 * @param fireEvents
	 *            <code>true</code> to allow selection events to be fired
	 */
	public void setSelectedItem(TreeItem item, boolean fireEvents) {
		if (item == null) {
			if (curSelection == null) {
				return;
			}
			curSelection.setSelected(false);
			curSelection = null;
			return;
		}
		onSelection(item, fireEvents, true);
	}

	@Override
	public void setTabIndex(int index) {
		FocusPanel.impl.setTabIndex(focusable, index);
	}

	/**
	 * Called only from {@link TreeItem}: Shows the closed image on that tree
	 * item.
	 *
	 * @param treeItem
	 *            the tree item
	 */
	void showClosedImage(TreeItem treeItem) {
		showImage(treeItem, images.treeClosed(), "closed");
	}

	private void showImage(TreeItem treeItem, AbstractImagePrototype proto,
			String className) {
		Element holder = treeItem.getImageHolderElement();
		Element child = DOM.getFirstChild(holder);
		if (child == null) {
			// If no image element has been created yet, create one from the
			// prototype.
			if (useCssTreeImages()) {
				child = Document.get().createSpanElement();
			} else {
				child = prototypeCache.elementFromPrototype(proto);
			}
			DOM.appendChild(holder, child);
		} else {
			if (treeItem.getCurrentImagePrototype(child) == proto
					|| useCssTreeImages()) {
			} else {
				// Otherwise, simply apply the prototype to the existing
				// element.
				prototypeCache.applyTo(proto, child);
			}
		}
		child.setClassName(className);
		treeItem.setCurrentImagePrototype(child, proto);
	}

	/**
	 * Called only from {@link TreeItem}: Shows the leaf image on a tree item.
	 *
	 * @param treeItem
	 *            the tree item
	 */
	void showLeafImage(TreeItem treeItem) {
		if (useLeafImages || treeItem.isFullNode()) {
			showImage(treeItem, images.treeLeaf(), "leaf");
		} else if (LocaleInfo.getCurrentLocale().isRTL()) {
			DOM.setStyleAttribute(treeItem.getElement(), "paddingRight",
					indentValue);
		} else {
			DOM.setStyleAttribute(treeItem.getElement(), "paddingLeft",
					indentValue);
		}
	}

	/**
	 * Called only from {@link TreeItem}: Shows the open image on a tree item.
	 *
	 * @param treeItem
	 *            the tree item
	 */
	void showOpenImage(TreeItem treeItem) {
		showImage(treeItem, images.treeOpen(), "open");
	}

	/**
	 * Iterator of tree items.
	 *
	 * @return the iterator
	 */
	public Iterator<TreeItem> treeItemIterator() {
		List<TreeItem> accum = new ArrayList<TreeItem>();
		root.addTreeItems(accum);
		return accum.iterator();
	}

	private void updateAriaAttributes() {
		Element curSelectionContentElem = curSelection.getContentElem();
		// Set the 'aria-level' state. To do this, we need to compute the level
		// of
		// the currently selected item.
		// We initialize itemLevel to -1 because the level value is zero-based.
		// Note that the root node is not a part of the TreeItem hierachy, and
		// we
		// do not consider the root node to have a designated level. The level
		// of
		// the root's children is level 0, its children's children is level 1,
		// etc.
		int curSelectionLevel = -1;
		TreeItem tempItem = curSelection;
		while (tempItem != null) {
			tempItem = tempItem.getParentItem();
			++curSelectionLevel;
		}
		Accessibility.setState(curSelectionContentElem,
				Accessibility.STATE_LEVEL,
				String.valueOf(curSelectionLevel + 1));
		// Set the 'aria-setsize' and 'aria-posinset' states. To do this, we
		// need to
		// compute the the number of siblings that the currently selected item
		// has,
		// and the item's position among its siblings.
		TreeItem curSelectionParent = curSelection.getParentItem();
		if (curSelectionParent == null) {
			curSelectionParent = root;
		}
		Accessibility.setState(curSelectionContentElem,
				Accessibility.STATE_SETSIZE,
				String.valueOf(curSelectionParent.getChildCount()));
		int curSelectionIndex = curSelectionParent.getChildIndex(curSelection);
		Accessibility.setState(curSelectionContentElem,
				Accessibility.STATE_POSINSET,
				String.valueOf(curSelectionIndex + 1));
		// Set the 'aria-expanded' state. This depends on the state of the
		// currently
		// selected item.
		// If the item has no children, we remove the 'aria-expanded' state.
		if (curSelection.getChildCount() == 0) {
			Accessibility.removeState(curSelectionContentElem,
					Accessibility.STATE_EXPANDED);
		} else {
			if (curSelection.getState()) {
				Accessibility.setState(curSelectionContentElem,
						Accessibility.STATE_EXPANDED, "true");
			} else {
				Accessibility.setState(curSelectionContentElem,
						Accessibility.STATE_EXPANDED, "false");
			}
		}
		// Make sure that 'aria-selected' is true.
		Accessibility.setState(curSelectionContentElem,
				Accessibility.STATE_SELECTED, "true");
		// Update the 'aria-activedescendant' state for the focusable element to
		// match the id of the currently selected item
		Accessibility.setState(focusable, Accessibility.STATE_ACTIVEDESCENDANT,
				DOM.getElementAttribute(curSelectionContentElem, "id"));
	}

	protected boolean useCssTreeImages() {
		return false;
	}

	class AbstractImagePrototypeCache {
		Map<AbstractImagePrototype, Element> localElementCache = new LinkedHashMap<>();

		public void applyTo(AbstractImagePrototype proto, Element child) {
			proto.applyTo(child.<ImagePrototypeElement> cast());
		}

		public Element elementFromPrototype(AbstractImagePrototype proto) {
			Element local = localElementCache.get(proto);
			if (local == null) {
				Element created = proto.createElement().<Element> cast();
				local = (Element) created.cloneNode(true);
				localElementCache.put(proto, local);
			}
			return (Element) local.cloneNode(true);
		}
	}

	/**
	 * There are several ways of configuring images for the Tree widget due to
	 * deprecated APIs.
	 */
	static class ImageAdapter {
		private static final Resources DEFAULT_RESOURCES = GWT
				.create(Resources.class);

		private final AbstractImagePrototype treeClosed;

		private final AbstractImagePrototype treeLeaf;

		private final AbstractImagePrototype treeOpen;

		public ImageAdapter() {
			this(DEFAULT_RESOURCES);
		}

		public ImageAdapter(Resources resources) {
			treeClosed = AbstractImagePrototype.create(resources.treeClosed());
			treeLeaf = AbstractImagePrototype.create(resources.treeLeaf());
			treeOpen = AbstractImagePrototype.create(resources.treeOpen());
		}

		public ImageAdapter(TreeImages images) {
			treeClosed = images.treeClosed();
			treeLeaf = images.treeLeaf();
			treeOpen = images.treeOpen();
		}

		public AbstractImagePrototype treeClosed() {
			return treeClosed;
		}

		public AbstractImagePrototype treeLeaf() {
			return treeLeaf;
		}

		public AbstractImagePrototype treeOpen() {
			return treeOpen;
		}
	}

	/**
	 * A ClientBundle that provides images for this widget.
	 */
	public interface Resources extends ClientBundle {
		/**
		 * An image indicating a closed branch.
		 */
		ImageResource treeClosed();

		/**
		 * An image indicating a leaf.
		 */
		ImageResource treeLeaf();

		/**
		 * An image indicating an open branch.
		 */
		ImageResource treeOpen();
		/**
		 * An image indicating an open branch.
		 */
	}
}
