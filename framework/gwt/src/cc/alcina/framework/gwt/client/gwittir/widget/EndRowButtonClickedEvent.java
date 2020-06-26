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
package cc.alcina.framework.gwt.client.gwittir.widget;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import cc.alcina.framework.gwt.client.gwittir.widget.EndRowButtonClickedEvent.EndRowButtonClickedHandler;

/**
 * Represents a close event.
 * 
 * @param the
 *            type being closed
 */
public class EndRowButtonClickedEvent
		extends GwtEvent<EndRowButtonClickedHandler> {
	/**
	 * Handler type.
	 */
	private static Type<EndRowButtonClickedHandler> TYPE = new Type<EndRowButtonClickedHandler>();

	public static void fire(HasEndRowClickedHandlers source, int rowIndex,
			Object rowObject) {
		EndRowButtonClickedEvent event = new EndRowButtonClickedEvent(rowIndex,
				rowObject);
		source.fireEvent(event);
	}

	/**
	 * Gets the type associated with this event.
	 * 
	 * @return returns the handler type
	 */
	public static Type<EndRowButtonClickedHandler> getType() {
		return TYPE;
	}

	private final int rowIndex;

	private final Object rowObject;

	/**
	 * Creates a new close event.
	 * 
	 * @param rowObject
	 * 
	 * @param target
	 *            the target
	 * @param collapsed
	 *            whether it is auto closed
	 */
	public EndRowButtonClickedEvent(int rowIndex, Object rowObject) {
		this.rowIndex = rowIndex;
		this.rowObject = rowObject;
	}

	// The instance knows its of type T, but the TYPE
	// field itself does not, so we have to do an unsafe cast here.
	@Override
	public final Type<EndRowButtonClickedHandler> getAssociatedType() {
		return (Type) TYPE;
	}

	public int getRowIndex() {
		return this.rowIndex;
	}

	public Object getRowObject() {
		return this.rowObject;
	}

	@Override
	protected void dispatch(EndRowButtonClickedHandler handler) {
		handler.onCollapse(this);
	}

	/**
	 * Handler interface for {@link EndRowButtonClickedEvent} events.
	 * 
	 */
	public interface EndRowButtonClickedHandler extends EventHandler {
		/**
		 * Called when {@link EndRowButtonClickedEvent} is fired.
		 * 
		 * @param event
		 *            the {@link EndRowButtonClickedEvent} that was fired
		 */
		void onCollapse(EndRowButtonClickedEvent event);
	}

	public interface HasEndRowClickedHandlers extends HasHandlers {
		/**
		 * Adds a {@link EndRowButtonClickedEvent} handler.
		 * 
		 * @param handler
		 *            the handler
		 * @return the registration for the event
		 */
		HandlerRegistration
				addEndRowClickedHandler(EndRowButtonClickedHandler handler);
	}
}
