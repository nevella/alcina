/*
 * Copyright 2011 Google Inc.
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
package cc.alcina.framework.gwt.client.dirndl.behaviour;

import com.google.gwt.event.dom.client.DomEvent;

public class DomSubmitEvent extends DomEvent<DomSubmitHandler> {
	private static final Type<DomSubmitHandler> TYPE = new Type<DomSubmitHandler>(
			"submit", new DomSubmitEvent());

	/**
	 * Gets the event type associated with drag end events.
	 * 
	 * @return the handler type
	 */
	public static Type<DomSubmitHandler> getType() {
		return TYPE;
	}

	protected DomSubmitEvent() {
	}

	@Override
	public final Type<DomSubmitHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(DomSubmitHandler handler) {
		handler.onDomSubmit(this);
	}
}
