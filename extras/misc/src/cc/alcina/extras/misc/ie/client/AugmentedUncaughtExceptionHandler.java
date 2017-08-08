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
package cc.alcina.extras.misc.ie.client;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.WrappedRuntimeException.SuggestedAction;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;

/**
 * Better stacktrace etc support for IE
 * 
 * @author nick@alcina.cc TODO
 */
public class AugmentedUncaughtExceptionHandler implements CloseHandler<Window> {
	private static UncaughtExceptionHandler handler;

	public void registerUncaughtExceptionHandler(
			UncaughtExceptionHandler handler) {
		if (GWT.isScript() && BrowserMod.isInternetExplorer()
				&& !BrowserMod.isIE10Plus()) {
			AugmentedUncaughtExceptionHandler.handler = handler;
			registerIEWindowErrorListener();
			Window.addCloseHandler(this);
			disableEventBusExceptionCatch();
		} else {
			GWT.setUncaughtExceptionHandler(handler);
		}
	}

	private native void disableEventBusExceptionCatch() /*-{
		$wnd.__com_google_web_bindery_event_shared_SimpleEventBus_disableEventBusExceptionCatch = true;
	}-*/;

	private native void registerIEWindowErrorListener() /*-{
		function AugmentedUncaughtExceptionHandler_windowErrorHandler(sMsg, sUrl, sLine) {
			var message = "\n\nMessage: " + sMsg + "\nLine: " + sLine + "\nUrl: " + sUrl;
			@cc.alcina.extras.misc.ie.client.AugmentedUncaughtExceptionHandler::throwToHandler(Ljava/lang/String;)(message);
		}
		$wnd.onerror = AugmentedUncaughtExceptionHandler_windowErrorHandler;
		window.onerror = AugmentedUncaughtExceptionHandler_windowErrorHandler;
	}-*/;

	public static void throwToHandler(String message) {
		List<Throwable> lastGwtThrowables = getLastThrowables();
		String gwtExceptionMessage = "";
		if (lastGwtThrowables != null) {
			StringBuilder builder = new StringBuilder();
			for (Throwable t : lastGwtThrowables) {
				if (handler instanceof ClientExceptionHandler) {
					if (((ClientExceptionHandler) handler)
							.handleNetworkException(t)) {
						return;
					}
				}
				ClientExceptionHandler.unrollUmbrella(t, builder);
			}
			gwtExceptionMessage = "\n\n-------\n(gwt)\n\n" + builder.toString();
		}
		handler.onUncaughtException(new WrappedRuntimeException(CommonUtils
				.formatJ("%s%s", message, gwtExceptionMessage),
				SuggestedAction.NOTIFY_WARNING));
	}

	private static native List<Throwable> getLastThrowables() /*-{
		return $wnd.__com_google_gwt_core_client_impl_Impl_ieThrowables;
	}-*/;

	public void onWindowClosed() {
		deregisterWindowErrorListener();
	}

	private native void deregisterWindowErrorListener() /*-{
		$wnd.onerror = null;
		window.onerror = null;
	}-*/;

	public void onClose(CloseEvent<Window> event) {
		deregisterWindowErrorListener();
	}
}