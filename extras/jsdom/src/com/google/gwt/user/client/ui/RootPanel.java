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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.BidiUtils;
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * The panel to which all other widgets must ultimately be added. RootPanels are
 * never created directly. Rather, they are accessed via {@link RootPanel#get()}
 * .
 *
 * <p>
 * Most applications will add widgets to the default root panel in their
 * {@link com.google.gwt.core.client.EntryPoint#onModuleLoad} methods.
 * </p>
 */
public class RootPanel extends AbsolutePanel {
	/**
	 * The singleton command used to detach widgets.
	 */
	private static final AttachDetachException.Command maybeDetachCommand = new AttachDetachException.Command() {
		@Override
		public void execute(Widget w) {
			if (w.isAttached()) {
				w.onDetach();
			}
		}
	};

	@Registration.Singleton
	@Registration.EnvironmentSingleton
	@Reflected
	public static class EnvironmentPanels {
		public static EnvironmentPanels get() {
			return Registry.impl(EnvironmentPanels.class);
		}

		private Map<String, RootPanel> rootPanels = new HashMap<String, RootPanel>();

		private Set<Widget> widgetsToDetach = new HashSet<Widget>();
	}

	// FIXME - dirndl - support for external attach/detach
	public static void attachNow(Widget widget) {
		assert !EnvironmentPanels.get().widgetsToDetach
				.contains(widget) : "attachNow() called on a widget "
						+ " currently in the detach list";
		widget.onAttach();
		EnvironmentPanels.get().widgetsToDetach.add(widget);
	}

	/**
	 * Marks a widget as detached and removes it from the detach list.
	 *
	 * <p>
	 * If an element belonging to a widget originally passed to
	 * {@link #detachOnWindowClose(Widget)} has been removed from the document,
	 * calling this method will cause it to be marked as detached immediately.
	 * Failure to do so will keep the widget from being garbage collected until
	 * the page is unloaded.
	 * </p>
	 *
	 * <p>
	 * This method may only be called per widget, and only for widgets that were
	 * originally passed to {@link #detachOnWindowClose(Widget)}.
	 * </p>
	 *
	 * @param widget
	 *            the widget that no longer needs to be cleaned up when the page
	 *            closes
	 * @see #detachOnWindowClose(Widget)
	 */
	public static void detachNow(Widget widget) {
		assert EnvironmentPanels.get().widgetsToDetach
				.contains(widget) : "detachNow() called on a widget "
						+ "not currently in the detach list";
		try {
			widget.onDetach();
		} finally {
			EnvironmentPanels.get().widgetsToDetach.remove(widget);
		}
	}

	/**
	 * Adds a widget to the detach list. This is the list of widgets to be
	 * detached when the page unloads.
	 *
	 * <p>
	 * This method must be called for all widgets that have no parent widgets.
	 * These are most commonly {@link RootPanel RootPanels}, but can also be any
	 * widget used to wrap an existing element on the page. Failing to do this
	 * may cause these widgets to leak memory. This method is called
	 * automatically by widgets' wrap methods (e.g.
	 * {@link Button#wrap(com.google.gwt.dom.client.Element)}).
	 * </p>
	 *
	 * <p>
	 * This method may <em>not</em> be called on any widget whose element is
	 * contained in another widget. This is to ensure that the DOM and Widget
	 * hierarchies cannot get into an inconsistent state.
	 * </p>
	 *
	 * @param widget
	 *            the widget to be cleaned up when the page closes
	 * @see #detachNow(Widget)
	 */
	public static void detachOnWindowClose(Widget widget) {
		assert !EnvironmentPanels.get().widgetsToDetach
				.contains(widget) : "detachOnUnload() called twice "
						+ "for the same widget";
		assert !isElementChildOfWidget(
				widget.getElement()) : "A widget that has "
						+ "an existing parent widget may not be added to the detach list";
		EnvironmentPanels.get().widgetsToDetach.add(widget);
	}

	// Package-protected for use by unit tests. Do not call this method
	// directly.
	static void detachWidgets() {
		// When the window is closing, detach all widgets that need to be
		// cleaned up. This will cause all of their event listeners
		// to be unhooked, which will avoid potential memory leaks.
		try {
			AttachDetachException.tryCommand(
					EnvironmentPanels.get().widgetsToDetach,
					maybeDetachCommand);
		} finally {
			EnvironmentPanels.get().widgetsToDetach.clear();
			// Clear the RootPanel cache, since we've "detached" all RootPanels
			// at
			// this point. This would be pointless, since it only happens on
			// unload,
			// but it is very helpful for unit tests, because it allows
			// RootPanel.get() to work properly even after a synthesized
			// "unload".
			EnvironmentPanels.get().rootPanels.clear();
		}
	}

	/**
	 * Gets the default root panel. This panel wraps the body of the browser's
	 * document. This root panel can contain any number of widgets, which will
	 * be laid out in their natural HTML ordering. Many applications, however,
	 * will add a single panel to the RootPanel to provide more structure.
	 *
	 * @return the default RootPanel
	 */
	public static RootPanel get() {
		return get(null);
	}

	/**
	 * Gets the root panel associated with a given browser element. For this to
	 * work, the HTML document into which the application is loaded must have
	 * specified an element with the given id.
	 *
	 * @param id
	 *            the id of the element to be wrapped with a root panel (
	 *            <code>null</code> specifies the default instance, which wraps
	 *            the &lt;body&gt; element)
	 * @return the root panel, or <code>null</code> if no such element was found
	 */
	public static RootPanel get(String id) {
		// See if this RootPanel is already created.
		RootPanel rp = EnvironmentPanels.get().rootPanels.get(id);
		// Find the element that this RootPanel will wrap.
		Element elem = null;
		if (id != null) {
			// Return null if the id is specified, but no element is found.
			if (null == (elem = Document.get().getElementById(id))) {
				return null;
			}
		}
		if (rp != null) {
			// If the element associated with an existing RootPanel has been
			// replaced
			// for any reason, return a new RootPanel rather than the existing
			// one (
			// see issue 1937).
			if ((elem == null) || (rp.getElement() == elem)) {
				// There's already an existing RootPanel for this element.
				// Return it.
				return rp;
			}
		}
		// Note that the code in this if block only happens once -
		// on the first RootPanel.get(String) or RootPanel.get()
		// call.
		if (EnvironmentPanels.get().rootPanels.size() == 0) {
			if (GWT.isClient()) {
				hookWindowClosing();
				// If we're in a RTL locale, set the RTL directionality
				// on the entire document.
				if (LocaleInfo.getCurrentLocale().isRTL()) {
					BidiUtils.setDirectionOnElement(getRootElement(),
							HasDirection.Direction.RTL);
				}
			}
		}
		// Create the panel and put it in the map.
		if (elem == null) {
			// 'null' means use document's body element.
			rp = new DefaultRootPanel();
		} else {
			// Otherwise, wrap the existing element.
			rp = new RootPanel(elem);
		}
		EnvironmentPanels.get().rootPanels.put(id, rp);
		detachOnWindowClose(rp);
		return rp;
	}

	public static Element getBodyElement() {
		return Document.get().getBody();
	}

	/**
	 * Convenience method for getting the document's root (<html>) element.
	 *
	 * @return the document's root element
	 */
	private static native Element getRootElement() /*-{
    return $doc;
	}-*/;

	private static void hookWindowClosing() {
		// Catch the window closing event.
		Window.addCloseHandler(new CloseHandler<Window>() {
			@Override
			public void onClose(CloseEvent<Window> closeEvent) {
				detachWidgets();
			}
		});
	}

	/*
	 * Checks to see whether the given element has any parent element that
	 * belongs to a widget. This is not terribly efficient, and is thus only
	 * used in an assertion.
	 */
	private static boolean isElementChildOfWidget(Element element) {
		// Walk up the DOM hierarchy, looking for any widget with an event
		// listener
		// set. Though it is not dependable in the general case that a widget
		// will
		// have set its element's event listener at all times, it *is*
		// dependable
		// if the widget is attached. Which it will be in this case.
		BodyElement body = Document.get().getBody();
		if (element == body) {
			return false;
		}
		element = element.getParentElement();
		while ((element != null) && (body != element)) {
			if (Event.getEventListener(element) != null) {
				return true;
			}
			element = element.getParentElement().cast();
		}
		return false;
	}

	/**
	 * Determines whether the given widget is in the detach list.
	 *
	 * @param widget
	 *            the widget to be checked
	 * @return <code>true</code> if the widget is in the detach list
	 */
	public static boolean isInDetachList(Widget widget) {
		return EnvironmentPanels.get().widgetsToDetach.contains(widget);
	}

	private RootPanel(Element elem) {
		super(elem);
		onAttach();
	}

	/**
	 * Clears the rootPanel. If clearDom is true, then also remove any DOM
	 * elements that are not widgets.
	 *
	 * <p>
	 * By default {@link #clear()} will only remove children that are GWT
	 * widgets. This method also provides the option to remove all children
	 * including the non-widget DOM elements that are directly added (e.g.
	 * elements added via {@code getElement().appendChild(...)}.
	 *
	 * @param clearDom
	 *            if {@code true} this method will also remove any DOM elements
	 *            that are not widgets.
	 */
	public void clear(boolean clearDom) {
		clear();
		if (clearDom) {
			getElement().removeAllChildren();
		}
	}

	/**
	 * A default RootPanel implementation that wraps the body element.
	 */
	private static class DefaultRootPanel extends RootPanel {
		public DefaultRootPanel() {
			super(getBodyElement());
		}

		@Override
		protected void setWidgetPositionImpl(Widget w, int left, int top) {
			// Account for the difference between absolute position and the
			// body's positioning context.
			left -= Document.get().getBodyOffsetLeft();
			top -= Document.get().getBodyOffsetTop();
			super.setWidgetPositionImpl(w, left, top);
		}
	}
}
