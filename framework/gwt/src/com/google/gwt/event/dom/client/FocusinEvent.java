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

public class FocusinEvent extends DomEvent<FocusinHandler> {
	private static final Type<FocusinHandler> TYPE = new Type<FocusinHandler>(
			BrowserEvents.FOCUSIN, new FocusinEvent());

	public static Type<FocusinHandler> getType() {
		return TYPE;
	}

	/**
	 * Protected constructor, use
	 * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
	 * to fire change events.
	 */
	protected FocusinEvent() {
	}

	@Override
	protected void dispatch(FocusinHandler handler) {
		handler.onFocusin(this);
	}

	@Override
	public final Type<FocusinHandler> getAssociatedType() {
		return TYPE;
	}
}
