package com.google.gwt.user.client;

import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

/**
 * History implementation using hash tokens.
 * <p>
 * This is the default implementation for all browsers except IE8.
 * 
 * NR - some pre-GWT2.6 methods copied back in here to support html5 history
 * module
 */
public class HistoryImpl {
	public HistoryImpl() {
		attachListener();
	}

	public void attachListener() {
		if (BrowserMod.isIE8()) {
			attachListenerIe8();
		} else {
			attachListenerStd();
		}
	}

	public native void attachListenerStd() /*-{
        // We explicitly use the third parameter for capture, since Firefox before version 6
        // throws an exception if the parameter is missing.
        // See: https://developer.mozilla.org/es/docs/DOM/elemento.addEventListener#Gecko_notes
        var handler = $entry(@com.google.gwt.user.client.History::onHashChanged());
        $wnd.addEventListener('hashchange', handler, false);
	}-*/;

	public native void attachListenerIe8() /*-{
        var handler = $entry(@com.google.gwt.user.client.History::onHashChanged());
        var oldHandler = $wnd.onhashchange;
        $wnd.onhashchange = function() {
            var ex;

            try {
                handler();
            } catch (e) {
                ex = e;
            }

            if (oldHandler != null) {
                try {
                    oldHandler();
                } catch (e) {
                    ex = ex || e;
                }
            }

            if (ex != null) {
                throw ex;
            }
        };
	}-*/;

	public void newToken(String historyToken) {
		nativeUpdate(historyToken);
	}

	public void replaceToken(String historyToken) {
		Window.Location.replace("#" + historyToken);
	}

	public boolean init() {
		return false;
	}

	public native void nativeUpdate(String historyToken) /*-{
        $wnd.location.hash = this.@com.google.gwt.user.client.HistoryImpl::encodeFragment(Ljava/lang/String;)(historyToken);
	}-*/;

	public native String decodeFragment(String encodedFragment) /*-{
        // decodeURI() does *not* decode the '#' character.
        return decodeURI(encodedFragment.replace("%23", "#"));
	}-*/;

	public void setToken(String token) {
		History.token = token;
	}

	public String getToken() {
		return History.token;
	}

	public native String encodeFragment(String fragment) /*-{
        // encodeURI() does *not* encode the '#' character.
        return encodeURI(fragment).replace("#", "%23");
	}-*/;

	public void fireHistoryChangedImpl(String token) {
		History.historyEventSource.fireValueChangedEvent(token);
	}

	public String encodeHistoryTokenWithHash(String targetHistoryToken) {
		return "#"+History.encodeHistoryToken(targetHistoryToken);
	}
}