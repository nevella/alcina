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
package cc.alcina.framework.gwt.client.util;

import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * 
 * <OL>
 * <LI>UTC date</LI>
 * <LI>CSS utils</LI>
 * 
 * </ol>
 * 
 * @author Nick Reddel
 * 
 */
public class ClientUtils {
	private static final String HEAD = "head";

	private static final String CSS_TEXT_PROPERTY = "cssText";

	public static boolean maybeOffline(Throwable t) {
		if (t instanceof WrappedRuntimeException) {
			t = t.getCause();
		}
		if (t instanceof StatusCodeException) {
			StatusCodeException sce = (StatusCodeException) t;
			ClientLayerLocator.get().notifications().log(
					"** Status code exception: " + sce.getStatusCode());
			boolean internetExplorerErrOffline = BrowserMod
					.isInternetExplorer()
					&& sce.getStatusCode() > 600;
			if ((!GWT.isScript() && sce.getStatusCode() == 500)
					|| sce.getStatusCode() == 0 || internetExplorerErrOffline) {
				return true;
			}
			// DNS error in Africa
			if (t.toString().contains("was not able to resolve the hostname")) {
				return true;
			}
		}
		return false;
	}

	public static Element updateCss(Element styleElement, String css) {
		if (styleElement == null) {
			styleElement = Document.get().createStyleElement();
			NodeList<Element> headList = Document.get().getElementsByTagName(
					HEAD);
			headList.getItem(0).appendChild(styleElement);
		}
		if (css.length() != 0) {
			try {
				styleElement.setInnerText(css);
			} catch (Exception e) {
				// fall through to IE
				try {
					styleElement.setPropertyString(CSS_TEXT_PROPERTY, css);
				} catch (Exception e1) {
					if (BrowserMod.isInternetExplorer()) {
						ClientLayerLocator
								.get()
								.notifications()
								.showMessage(
										"Sorry, this action is not supported "
												+ "on some versions of Internet Explorer");
					}
				}
			}
		}
		return styleElement;
	}

	private static void addHidden(Panel p, String key, String value) {
		p.add(new Hidden(key, value));
	}

	public static void submitForm(Map<String, String> params, String url) {
		FormPanel p = new FormPanel((String) null);
		p.setAction(url);
		p.setMethod(FormPanel.METHOD_POST);
		FlowPanel fp = new FlowPanel();
		p.add(fp);
		for (String key : params.keySet()) {
			addHidden(fp, key, params.get(key));
		}
		RootPanel.get().add(p);
		p.submit();
		p.removeFromParent();
	}

	public static void notImplemented() {
		ClientLayerLocator.get().notifications().showWarning(
				"Not yet implemented");
	}
	public static UrlBuilder getBaseUrlBuilder(){
		UrlBuilder builder =new UrlBuilder();
		builder.setProtocol(Window.Location.getProtocol());
		builder.setHost(Window.Location.getHostName());
		String port = Window.Location.getPort();
		if (port != null && port.length() > 0) {
	        builder.setPort(Integer.parseInt(port));
	      }
		return builder;
	}
	public static native void invokeJsDebugger() /*-{
		debugger;
	}-*/;
	public static void fireHistoryToken(String token){
		if (token==null){
			return;
		}
		if (token.equals(History.getToken())){
			History.fireCurrentHistoryState();
		}
		History.newItem(token);
	}
}
