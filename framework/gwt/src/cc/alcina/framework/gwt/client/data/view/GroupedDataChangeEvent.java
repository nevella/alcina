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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.gwt.client.data.search.DataSearchDefinition;

/**
 * Event thrown when data provider's data
 */
public class GroupedDataChangeEvent<T extends GroupedResult>
		extends GwtEvent<GroupedDataChangeEvent.Handler> {
	/**
	 * A singleton instance of Type&lt;Handler&gt;.
	 */
	public static final Type<Handler> TYPE = new Type<Handler>();

	private T value;

	private DataSearchDefinition def;

	public DataSearchDefinition getDef() {
		return this.def;
	}

	/**
	 * @param def
	 */
	public GroupedDataChangeEvent(T value, DataSearchDefinition def) {
		this.value = value;
		this.def = def;
	}

	@Override
	public Type<Handler> getAssociatedType() {
		return TYPE;
	}

	public T getValue() {
		return this.value;
	}

	@Override
	protected void dispatch(Handler handler) {
		handler.onGroupedDataChange(this);
	}

	public interface Handler extends EventHandler {
		/**
		 * Called when a {@link GroupedDataChangeEvent} is fired.
		 *
		 * @param event
		 *            the {@link GroupedDataChangeEvent}
		 */
		void onGroupedDataChange(GroupedDataChangeEvent event);
	}

	public interface HasDataChangeHandlers<T> extends HasHandlers {
		HandlerRegistration
				addDataChangeHandler(GroupedDataChangeEvent.Handler handler);
	}
}
