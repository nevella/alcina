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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import cc.alcina.framework.gwt.client.widget.PopupShownEvent.PopupShownHandler;

/**
 * Represents a popup shown event.
 * 
 */
public class PopupShownEvent extends GwtEvent<PopupShownHandler> {
	/**
	 * Handler type.
	 */
	private static Type<PopupShownHandler> TYPE = new Type<PopupShownHandler>();

	public static void fire(HasPopupShownHandlers source, boolean shown) {
		PopupShownEvent event = new PopupShownEvent(shown);
		source.fireEvent(event);
	}

	/**
	 * Gets the type associated with this event.
	 * 
	 * @return returns the handler type
	 */
	public static Type<PopupShownHandler> getType() {
		return TYPE;
	}

	private final boolean shown;

	/**
	 * Creates a new popupshown event.
	 * 
	 * @param shown
	 * 
	 */
	public PopupShownEvent(boolean shown) {
		this.shown = shown;
	}

	// The instance knows its of type T, but the TYPE
	// field itself does not, so we have to do an unsafe cast here.
	@Override
	public final Type<PopupShownHandler> getAssociatedType() {
		return (Type) TYPE;
	}

	public boolean isShown() {
		return this.shown;
	}

	@Override
	protected void dispatch(PopupShownHandler handler) {
		handler.onPopupShown(this);
	}

	public interface HasPopupShownHandlers extends HasHandlers {
		/**
		 * Adds a {@link PopupShownEvent} handler.
		 * 
		 * @param handler
		 *            the handler
		 * @return the registration for the event
		 */
		HandlerRegistration addPopupShownHandler(PopupShownHandler handler);
	}

	/**
	 * Handler interface for {@link PopupShownEvent} events.
	 * 
	 */
	public interface PopupShownHandler extends EventHandler {
		/**
		 * Called when {@link PopupShownEvent} is fired.
		 * 
		 * @param event
		 *            the {@link PopupShownEvent} that was fired
		 */
		void onPopupShown(PopupShownEvent event);
	}
}
