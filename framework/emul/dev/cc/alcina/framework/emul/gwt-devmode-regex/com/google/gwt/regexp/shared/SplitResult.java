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
package com.google.gwt.regexp.shared;

import com.google.gwt.core.client.GwtScriptOnly;
import com.google.gwt.core.client.JsArrayString;

/**
 * GWT wrapper for Javascript string.split results.
 */
public class SplitResult {
	private ISplitResult impl;

	public SplitResult(ISplitResult impl) {
		this.impl = impl;
	}

	public final String get(int index) {
		return this.impl.get(index);
	}


	public final int length() {
		return this.impl.length();
	}


	public final void set(int index, String value) {
		this.impl.set(index, value);
	}
	
}
