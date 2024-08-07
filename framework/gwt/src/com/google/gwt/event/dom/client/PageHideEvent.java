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
package com.google.gwt.event.dom.client;

import com.google.gwt.dom.client.BrowserEvents;

/**
 * Represents a native pageHide event.
 */
public class PageHideEvent extends DomEvent<PageHideHandler> {
	/**
	 * Event type for pageHide events. Represents the meta-data associated with
	 * this event.
	 */
	private static final Type<PageHideHandler> TYPE = new Type<PageHideHandler>(
			BrowserEvents.PAGEHIDE, new PageHideEvent());

	/**
	 * Gets the event type associated with change events.
	 *
	 * @return the handler type
	 */
	public static Type<PageHideHandler> getType() {
		return TYPE;
	}

	public PageHideEvent() {
	}

	@Override
	protected void dispatch(PageHideHandler handler) {
		handler.onPageHide(this);
	}

	@Override
	public final Type<PageHideHandler> getAssociatedType() {
		return TYPE;
	}
}
