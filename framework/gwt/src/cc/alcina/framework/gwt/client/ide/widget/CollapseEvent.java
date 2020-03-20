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
package cc.alcina.framework.gwt.client.ide.widget;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import cc.alcina.framework.gwt.client.ide.widget.CollapseEvent.CollapseHandler;

/**
 * Represents a close event.
 * 
 * @param <T>
 *            the type being closed
 */
public class CollapseEvent<T> extends GwtEvent<CollapseHandler<T>> {
	/**
	 * Handler type.
	 */
	private static Type<CollapseHandler<?>> TYPE;

	/**
	 * Fires a close event on all registered handlers in the handler manager. If
	 * no such handlers exist, this method will do nothing.
	 * 
	 * @param <T>
	 *            the target type
	 * @param source
	 *            the source of the handlers
	 * @param target
	 *            the target
	 */
	public static <T> void fire(HasCollapseHandlers<T> source, T target) {
		fire(source, target, false);
	}

	/**
	 * Fires a close event on all registered handlers in the handler manager.
	 * 
	 * @param <T>
	 *            the target type
	 * @param source
	 *            the source of the handlers
	 * @param target
	 *            the target
	 * @param autoClosed
	 *            was the target closed automatically
	 */
	public static <T> void fire(HasCollapseHandlers<T> source, T target,
			boolean autoClosed) {
		if (TYPE != null) {
			CollapseEvent<T> event = new CollapseEvent<T>(target, autoClosed);
			source.fireEvent(event);
		}
	}

	/**
	 * Gets the type associated with this event.
	 * 
	 * @return returns the handler type
	 */
	public static Type<CollapseHandler<?>> getType() {
		return TYPE != null ? TYPE : (TYPE = new Type<CollapseHandler<?>>());
	}

	private final T target;

	private final boolean collapsed;

	/**
	 * Creates a new close event.
	 * 
	 * @param target
	 *            the target
	 * @param collapsed
	 *            whether it is auto closed
	 */
	public CollapseEvent(T target, boolean collapsed) {
		this.collapsed = collapsed;
		this.target = target;
	}

	// The instance knows its of type T, but the TYPE
	// field itself does not, so we have to do an unsafe cast here.
	
	@Override
	public final Type<CollapseHandler<T>> getAssociatedType() {
		return (Type) TYPE;
	}

	/**
	 * Gets the target.
	 * 
	 * @return the target
	 */
	public T getTarget() {
		return target;
	}

	/**
	 * Was the target collapsed (or expanded)?
	 * 
	 */
	public boolean isCollapsed() {
		return collapsed;
	}

	@Override
	protected void dispatch(CollapseHandler<T> handler) {
		handler.onCollapse(this);
	}

	/**
	 * Handler interface for {@link CollapseEvent} events.
	 * 
	 * @param <T>
	 *            the type being closed
	 */
	public interface CollapseHandler<T> extends EventHandler {
		/**
		 * Called when {@link CollapseEvent} is fired.
		 * 
		 * @param event
		 *            the {@link CollapseEvent} that was fired
		 */
		void onCollapse(CollapseEvent<T> event);
	}

	/**
	 * A widget that implements this interface is a public source of
	 * {@link CollapseEvent} events.
	 * 
	 * @param <T>
	 *            the type being closed
	 */
	public interface HasCollapseHandlers<T> extends HasHandlers {
		/**
		 * Adds a {@link CollapseEvent} handler.
		 * 
		 * @param handler
		 *            the handler
		 * @return the registration for the event
		 */
		HandlerRegistration addCollapseHandler(CollapseHandler<T> handler);
	}
}
