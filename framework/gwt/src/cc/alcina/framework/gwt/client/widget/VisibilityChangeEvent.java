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
package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.place.shared.Place;

/**
 * Event thrown when the widget's visibility changes
 */
public class VisibilityChangeEvent extends
		GwtEvent<VisibilityChangeEvent.Handler> {
	/**
	 * A singleton instance of Type&lt;Handler&gt;.
	 */
	public static final Type<Handler> TYPE = new Type<Handler>();

	private boolean visible;

	/**
	 */
	public VisibilityChangeEvent(boolean visible) {
		this.visible = visible;
	}

	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	public boolean isVisible() {
		return this.visible;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onVisiblityChange(this);
	}

	public interface Handler extends EventHandler {
		/**
		 * Called when a {@link VisibilityChangeEvent} is fired.
		 *
		 * @param event
		 *            the {@link VisibilityChangeEvent}
		 */
		void onVisiblityChange(VisibilityChangeEvent event);
	}

	public interface HasVisibilityChangeHandlers extends HasHandlers {
		HandlerRegistration addVisibilityChangeHandler(
				VisibilityChangeEvent.Handler handler);
	}
}
