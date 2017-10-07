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
package com.google.gwt.user.cellview.client;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * Standard implementation used by most cell based widgets.
 */
class CellBasedWidgetImplStandard extends CellBasedWidgetImpl {
	private class SinkHandler implements Handler {
		public HandlerRegistration registration;

		private Element elem;

		private String typeName;

		public SinkHandler(Element elem, String typeName) {
			this.elem = elem;
			this.typeName = typeName;
		}

		@Override
		public void onAttachOrDetach(AttachEvent event) {
			Preconditions.checkArgument(event.isAttached());
			Scheduler.get().scheduleFinally(() -> sinkEventImpl(
					elem.implAccess().ensureRemote(), typeName));
			registration.removeHandler();
		}
	}

	/**
	 * The method used to dispatch non-bubbling events.
	 */
	private static JavaScriptObject dispatchNonBubblingEvent;

	/**
	 * Handle an event from a cell. Used by {@link #initEventSystem()}.
	 *
	 * @param event
	 *            the event to handle.
	 */
	private static void handleNonBubblingEvent(Event event) {
		// Get the event target.
		EventTarget eventTarget = event.getEventTarget();
		if (!Element.is(eventTarget)) {
			return;
		}
		Element target = eventTarget.cast();
		// Get the event listener, which is the first widget that handles the
		// specified event type.
		String typeName = event.getType();
		EventListener listener = DOM.getEventListener(target);
		while (target != null && listener == null
				&& target.getParentElement() != null) {
			target = target.getParentElement().cast();
			if (target != null && isNonBubblingEventHandled(target, typeName)) {
				// The target handles the event, so this must be the event
				// listener.
				listener = DOM.getEventListener(target);
			}
		}
		// Fire the event.
		if (listener != null) {
			DOM.dispatchEvent(event, target, listener);
		}
	}

	/**
	 * Check if the specified element handles the a non-bubbling event.
	 *
	 * @param elem
	 *            the element to check
	 * @param typeName
	 *            the non-bubbling event
	 * @return true if the event is handled, false if not
	 */
	private static boolean isNonBubblingEventHandled(Element elem,
			String typeName) {
		return "true".equals(elem.getAttribute(
				"__gwtCellBasedWidgetImplDispatching" + typeName));
	}

	/**
	 * The set of non bubbling event types.
	 */
	private final Set<String> nonBubblingEvents;

	public CellBasedWidgetImplStandard() {
		// Initialize the set of non-bubbling events.
		nonBubblingEvents = new HashSet<String>();
		nonBubblingEvents.add(BrowserEvents.FOCUS);
		nonBubblingEvents.add(BrowserEvents.BLUR);
		nonBubblingEvents.add(BrowserEvents.LOAD);
		nonBubblingEvents.add(BrowserEvents.ERROR);
	}

	@Override
	protected int sinkEvent(Widget widget, String typeName) {
		if (nonBubblingEvents.contains(typeName)) {
			// Initialize the event system.
			if (dispatchNonBubblingEvent == null) {
				initEventSystem();
			}
			// Sink the non-bubbling event.
			Element elem = widget.getElement();
			if (!isNonBubblingEventHandled(elem, typeName)) {
				elem.setAttribute(
						"__gwtCellBasedWidgetImplDispatching" + typeName,
						"true");
				SinkHandler handler = new SinkHandler(elem, typeName);
				HandlerRegistration registration = widget
						.addAttachHandler(handler);
				handler.registration = registration;
			}
			return -1;
		} else {
			return super.sinkEvent(widget, typeName);
		}
	}

	@Override
	public void resetFocus(ScheduledCommand command) {
		// Some browsers will not focus an element that was created in this
		// event loop.
		Scheduler.get().scheduleDeferred(command);
	}

	/**
	 * Initialize the event system.
	 */
	private native void initEventSystem() /*-{
        @com.google.gwt.user.cellview.client.CellBasedWidgetImplStandard::dispatchNonBubblingEvent = $entry(function(
                evt) {
            @com.google.gwt.user.cellview.client.CellBasedWidgetImplStandard::handleNonBubblingEvent(Lcom/google/gwt/user/client/Event;)(evt);
        });
	}-*/;

	/**
	 * Sink an event on the element.
	 *
	 * @param elem
	 *            the element to sink the event on
	 * @param typeName
	 *            the name of the event to sink
	 */
	private native void sinkEventImpl(ElementRemote elem, String typeName) /*-{
        elem
                .addEventListener(
                        typeName,
                        @com.google.gwt.user.cellview.client.CellBasedWidgetImplStandard::dispatchNonBubblingEvent,
                        true);
	}-*/;
}
