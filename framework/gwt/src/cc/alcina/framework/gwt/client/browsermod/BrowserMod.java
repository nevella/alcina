/*
 * Copyright Miroslav Pokorny
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
package cc.alcina.framework.gwt.client.browsermod;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;

/**
 * A collection of helper methods related to the browser, often reporting values
 * retrieved from the known browser properties.
 * 
 * @author Miroslav Pokorny (mP)
 * 
 *         Altered - Nick Reddel, trimmed everything down to basic UA stuff (to
 *         avoid jar dependency) fixed a few incorrectly-ordered arguments
 */
public class BrowserMod {
	/**
	 * Retrieves the window object for the current page.
	 * 
	 * @return The window
	 */
	public static native JavaScriptObject getWindow() /*-{
		return $wnd;
	}-*/;

	/**
	 * Scrolls the top left of the window to the position denoted by the given
	 * x/y coordinates
	 * 
	 * @param x
	 *            The horizontal offset in pixels
	 * @param y
	 *            The vertical offset in pixels
	 */
	public static native void scrollTo(final int x, final int y)/*-{
		$wnd.scroll(x, y);
	}-*/;

	/**
	 * Returns the contextPath of this web application, this concept is
	 * particularly useful for working with J2EE web applications.
	 * 
	 * @return The context path for this application.
	 */
	public static String getContextPath() {
		String url = GWT.getModuleBaseURL();
		if (GWT.isScript()) {
			final String path = Window.Location.getPath();
			final int webContextEnd = path.indexOf('/', 0);
			url = path.substring(0, webContextEnd);
		}
		// drop trailing slash if one is present.
		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	/**
	 * Returns the available screen area within the browser
	 * 
	 * @return The width in pixels
	 */
	public native static int getAvailableScreenWidth()/*-{
		return $wnd.screen.availWidth;
	}-*/;

	/**
	 * Returns the available screen area within the browser
	 * 
	 * @return The height in pixels.
	 */
	public native static int getAvailableScreenHeight()/*-{
		return $wnd.screen.availHeight;
	}-*/;

	public static boolean isInternetExplorer() {
		return getUserAgent().indexOf(Constants.INTERNET_EXPLORER_USER_AGENT) != -1
				&& !isOpera() && !isSafari() && !isChrome();
	}

	public static boolean isIE8() {
		return isInternetExplorer()
				&& getUserAgent().indexOf(
						Constants.INTERNET_EXPLORER_8_USER_AGENT) != -1;
	}

	public static boolean isIEpre8() {
		return isInternetExplorer() && !isIE8();
	}

	public static boolean isFireFox() {
		return getUserAgent().indexOf(Constants.FIREFOX_USER_AGENT) != -1
				&& !isOpera();
	}

	public static boolean isOpera8() {
		return getUserAgent().indexOf(Constants.OPERA8_USER_AGENT) != -1;
	}

	public static boolean isOpera9() {
		return getUserAgent().indexOf(Constants.OPERA9_USER_AGENT) != -1;
	}

	public static boolean isOpera() {
		return getUserAgent().indexOf(Constants.OPERA_USER_AGENT) != -1;
	}

	public static boolean isSafari() {
		return getUserAgent().indexOf(Constants.SAFARI_USER_AGENT) != -1
				&& !isOpera() && !isChrome();
	}

	public static boolean isChrome() {
		return getUserAgent().indexOf(Constants.CHROME_USER_AGENT) != -1
				&& !isOpera();
	}

	/**
	 * Retrieves the userAgent of the browser
	 * 
	 * @return the reported user agent
	 */
	public static native String getUserAgent()/*-{
		return $wnd.navigator.userAgent;
	}-*/;

	/**
	 * Returns the host operating system that the browser is running under.
	 * 
	 * @return The host operating system.
	 */
	public static String getOperatingSystem() {
		final String userAgent = BrowserMod.getUserAgent();
		final int leftParenthesis = userAgent.indexOf('(');
		int semiColonOrRightParen = userAgent.indexOf(';', leftParenthesis);
		int rightParenthesis = userAgent.indexOf(')', leftParenthesis);
		if (semiColonOrRightParen == -1) {
			semiColonOrRightParen = rightParenthesis;
		}
		return userAgent.substring(leftParenthesis + 1, semiColonOrRightParen);
	}

	/**
	 * Only warn the user if in hosted mode and the browser host page causes the
	 * document to be rendered in quirks mode.
	 */
	static {
		if (false == GWT.isScript() && BrowserMod.isQuirksMode()) {
			GWT.log(Constants.QUIRKS_MODE_WARNING, null);
		}
	}

	/**
	 * This method tests if the browser is in quirks mode.
	 * 
	 * @return true if the browser is operating in quirks mode otherwise returns
	 *         false
	 */
	native static public boolean isQuirksMode()/*-{
		return "BackCompat" == $doc.compatMode;
	}-*/;
}