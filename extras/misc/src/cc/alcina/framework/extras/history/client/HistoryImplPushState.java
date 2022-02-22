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

import java.util.Objects;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryImpl;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.place.BasePlace;

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
 */
public class HistoryImplPushState extends HistoryImpl {
	/**
	 * Add the given token to the history using pushState.
	 */
	private static native void pushState(final String token) /*-{
    var state = {
      historyToken : token
    };
    $wnd.history.pushState(state, $doc.title, token);
	}-*/;

	private static native void replaceState(final String token) /*-{
    var state = {
      historyToken : token
    };
    $wnd.history.replaceState(state, $doc.title, token);
	}-*/;

	private String lastPushed = null;

	@Override
	public native String decodeFragment(String encodedFragment) /*-{
    return decodeURI(encodedFragment.replace("%23", "#"));
	}-*/;

	@Override
	public boolean init() {
		// initialize HistoryImpl with the current path
		String initialToken = Window.Location.getPath()
				+ Window.Location.getQueryString();
		// force a push of initialtoken
		// lastPushed = initialToken;
		if (!Window.Location.getHash().isEmpty()) {
			String hash = Window.Location.getHash();
			if (Registry.optional(AlcinaHistory.class).isPresent()) {
				AlcinaHistory.initialiseDebugIds();
			}
			if (hash.startsWith("#")) {
				hash = hash.substring(1);
			}
			if (hash.startsWith("!")) {
				hash = hash.substring(1);
			}
			if (hash.startsWith("/")) {
				initialToken = hash;
			}
		}
		updateHistoryToken(initialToken);
		// initialize the empty state with the current history token
		nativeUpdate(getToken());
		// initialize the popState handler
		initPopStateHandler();
		return true;
	}

	@Override
	public void nativeUpdate(final String historyToken) {
		String newPushStateToken = CodeServerParameterHelper
				.append(historyToken);
		if (!newPushStateToken.startsWith("/")) {
			newPushStateToken = "/" + newPushStateToken;
		}
		if (!Objects.equals(newPushStateToken, lastPushed)
				&& !Objects.equals(historyToken, lastPushed)) {
			pushState(newPushStateToken);
			lastPushed = newPushStateToken;
		}
	}

	/**
	 * Initialize an event handler that gets executed when the token changes.
	 */
	private native void initPopStateHandler() /*-{
    var that = this;
    var oldHandler = $wnd.onpopstate;
    $wnd.onpopstate = $entry(function(e) {
      if (e.state && e.state.historyToken) {
        that.@cc.alcina.framework.extras.history.client.HistoryImplPushState::onPopState(Ljava/lang/String;)(e.state.historyToken);
      }
      if (oldHandler) {
        oldHandler(e);
      }
    });
	}-*/;

	/**
	 * Called from native JavaScript when an old history state was popped.
	 */
	private void onPopState(final String historyToken) {
		lastPushed = null;
		updateHistoryToken(historyToken);
		fireHistoryChangedImpl(getToken());
	}

	/**
	 * Set the current path as GWT History token which can later retrieved with
	 * {@link History#getToken()}.
	 */
	private void updateHistoryToken(String path) {
		String[] split = path.split("\\?");
		String token = split[0];
		token = (token.length() > 0) ? decodeFragment(token) : "";
		token = (token.startsWith("/")) ? token.substring(1) : token;
		String queryString = (split.length == 2) ? split[1] : "";
		queryString = CodeServerParameterHelper.remove(queryString);
		if (queryString != null && !queryString.trim().isEmpty()) {
			token += "?" + queryString;
		}
		setToken(token);
	}

	@Registration(value = BasePlace.HrefProvider.class, priority = Registration.Priority.REMOVE)
	public static class HrefProviderPushState extends BasePlace.HrefProvider {
		@Override
		public String toHrefString(BasePlace basePlace) {
			String path = "/" + BasePlace.tokenFor(basePlace);
			path = CodeServerParameterHelper.append(path);
			return path;
		}
	}
}
