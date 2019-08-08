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
package cc.alcina.framework.gwt.client.data.view;

import java.util.List;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

/**
 * Event thrown when data provider's data
 */
public class DataChangeEvent<T> extends GwtEvent<DataChangeEvent.Handler> {
	/**
	 * A singleton instance of Type&lt;Handler&gt;.
	 */
	public static final Type<Handler> TYPE = new Type<Handler>();

	private List<T> values;

	/**
	 */
	public DataChangeEvent(List<T> values) {
		this.values = values;
	}

	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	public List<T> getValues() {
		return this.values;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onDataChange(this);
	}

	public interface Handler extends EventHandler {
		/**
		 * Called when a {@link DataChangeEvent} is fired.
		 *
		 * @param event
		 *            the {@link DataChangeEvent}
		 */
		void onDataChange(DataChangeEvent event);
	}

	public interface HasDataChangeHandlers<T> extends HasHandlers {
		HandlerRegistration
				addDataChangeHandler(DataChangeEvent.Handler handler);
	}
}
