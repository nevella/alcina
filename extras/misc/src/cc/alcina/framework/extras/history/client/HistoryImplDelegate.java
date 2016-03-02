/*
 * Copyright 2012 Johannes Barop
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.extras.history.client;

import java.util.logging.Logger;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryImpl;
import com.google.gwt.user.client.Window;

/**
 * Extends GWT's {@link HistoryImpl} and adds HTML5 pushState support.
 * 
 * <p>
 * The complete path is treated as history token.
 * </p>
 * 
 * <p>
 * The leading '/' is hidden from GWTs History API, so that the path '/' is
 * returned as an empty history token ('').
 * </p>
 * 
 * @author <a href="mailto:jb@barop.de">Johannes Barop</a>
 * 
 */
public class HistoryImplDelegate extends HistoryImpl {
	public static native boolean isHtml5() /*-{
        return (typeof (window.history.pushState) == "function");
	}-*/;

	HistoryImpl impl;

	public HistoryImplDelegate() {
		ensureImpl();
	}

	@Override
	public void attachListener() {
		ensureImpl();
		impl.attachListener();
	}

	@Override
	public String decodeFragment(String token) {
		return impl.decodeFragment(token);
	}

	@Override
	public String encodeFragment(String token) {
		return impl.encodeFragment(token);
	}

	@Override
	public String encodeHistoryTokenWithHash(String targetHistoryToken) {
		// no hash
		return History.encodeHistoryToken(targetHistoryToken);
	}

	@Override
	public void fireHistoryChangedImpl(String token) {
		impl.fireHistoryChangedImpl(token);
	}

	@Override
	public String getToken() {
		return impl.getToken();
	}

	@Override
	public boolean init() {
		return impl.init();
	}

	@Override
	public void nativeUpdate(String historyToken) {
		impl.nativeUpdate(historyToken);
	}

	@Override
	public void newToken(String historyToken) {
		impl.newToken(historyToken);
	}

	@Override
	public void replaceToken(String historyToken) {
		impl.replaceToken(historyToken);
	}

	@Override
	public void setToken(String token) {
		impl.setToken(token);
	}

	private void ensureImpl() {
		if (impl == null) {
			if (isHtml5()) {
				impl = new HistoryImplPushState();
			} else {
				impl = new HistoryImpl();
			}
		}
	}
}
