/* 
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

package cc.alcina.framework.gwt.client;


import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;

/**
 * Better stacktrace etc support for IE
 * 
 * @author nick@alcina.cc TODO
 */
public class AugmentedUncaughtExceptionHandler implements CloseHandler<Window> {
	private static UncaughtExceptionHandler handler;

	public void registerUncaughtExceptionHandler(
			UncaughtExceptionHandler handler) {
		if (GWT.isScript() && BrowserMod.isInternetExplorer()) {
			AugmentedUncaughtExceptionHandler.handler = handler;
			registerIEWindowErrorListener();
			Window.addCloseHandler(this);
		} else {
			GWT.setUncaughtExceptionHandler(handler);
		}
	}

	private native void registerIEWindowErrorListener() /*-{
		function AugmentedUncaughtExceptionHandler_windowErrorHandler(sMsg,sUrl,sLine){
			var message = "\n\nMessage: "+sMsg + "\nLine: "+sLine+"\nUrl: "+sUrl;
			@cc.alcina.framework.gwt.client.AugmentedUncaughtExceptionHandler::throwToHandler(Ljava/lang/String;)(message);
		}
		$wnd.onerror=AugmentedUncaughtExceptionHandler_windowErrorHandler;
		window.onerror=AugmentedUncaughtExceptionHandler_windowErrorHandler;
	}-*/;

	public static void throwToHandler(String message) {
		handler.onUncaughtException(new WrappedRuntimeException(message,
				SuggestedAction.NOTIFY_WARNING));
	}

	public void onWindowClosed() {
		deregisterWindowErrorListener();
	}

	private native void deregisterWindowErrorListener() /*-{
		$wnd.onerror=null;
		window.onerror=null;
	}-*/;

	public void onClose(CloseEvent<Window> event) {
		deregisterWindowErrorListener();
	}
}
