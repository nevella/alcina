/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * The native dom event.
 */
public class NativeEventJso extends JavaScriptObject {
	/**
	 * The left mouse button.
	 */
	public static final int BUTTON_LEFT = 1;

	/**
	 * The middle mouse button.
	 */
	public static final int BUTTON_MIDDLE = 4;

	/**
	 * The right mouse button.
	 */
	public static final int BUTTON_RIGHT = 2;

	/**
	 * Required constructor for GWT compiler to function.
	 */
	protected NativeEventJso() {
	}

	public final NativeEvent asNativeEvent() {
		return new NativeEvent(this);
	}

	/**
	 * Get the {@link DataTransfer} associated with the current drag event.
	 * 
	 * @return the {@link DataTransfer} object, or null if not a drag event
	 */
	public final native DataTransfer getDataTransfer() /*-{
    return this.dataTransfer || null;
	}-*/;
}
