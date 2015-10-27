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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.OkCancelPanel;
import cc.alcina.framework.gwt.client.ide.ContentViewFactory.PaneWrapperWithObjects;
import cc.alcina.framework.gwt.client.logic.AlcinaDebugIds;
import cc.alcina.framework.gwt.client.widget.RelativePopupValidationFeedback;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.totsp.gwittir.client.beans.Binding;

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
		while (t instanceof WrappedRuntimeException) {
			if (t == t.getCause() || t.getCause() == null) {
				break;
			}
			t = t.getCause();
		}
		if (t.getMessage() != null
				&& t.getMessage().contains(
						"IOException while sending RPC request")) {
			return true;
		}
		if (t.getMessage() != null
				&& t.getMessage().contains(
						"IOException while receiving RPC response")) {
			return true;
		}
		if (t instanceof StatusCodeException) {
			if (AlcinaDebugIds.hasFlag(AlcinaDebugIds.DEBUG_SIMULATE_OFFLINE)) {
				return true;
			}
			StatusCodeException sce = (StatusCodeException) t;
			Registry.impl(ClientNotifications.class).log(
					"** Status code exception: " + sce.getStatusCode());
			if (sce.getStatusCode() == 0) {
				return true;
			}
			boolean internetExplorerErrOffline = BrowserMod
					.isInternetExplorer() && sce.getStatusCode() > 600;
			if (internetExplorerErrOffline) {
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
			if (headList == null || headList.getLength() == 0) {
				// something wrong with the client here -- bail
				AlcinaTopics.notifyDevWarning(new Exception("headList - "
						+ headList == null ? "null" : "length 0"));
				return null;
			}
			headList.getItem(0).appendChild(styleElement);
		}
		if (css.length() != 0) {
			try {
				if (!setCssTextViaCssTextProperty(styleElement, css)) {
					styleElement.setInnerText(css);
				}
			} catch (Exception e) {
				// squelch
			}
		}
		return styleElement;
	}

	public static native String stringify(JavaScriptObject jso) /*-{
        return JSON.stringify(jso);
	}-*/;

	public static native boolean setCssTextViaCssTextProperty(Element styleTag,
			String css) /*-{
        var sheet = styleTag.sheet ? styleTag.sheet : styleTag.styleSheet;

        if ('cssText' in sheet) { // Internet Explorer
            sheet.cssText = css;
            return true;
        }

        return false;//do innerText
	}-*/;

	private static void addHidden(Panel p, String key, String value) {
		p.add(new Hidden(key, value));
	}

	public static void submitForm(Map<String, String> params, String url) {
		FormPanel p = new FormPanel("_self");
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
		Registry.impl(ClientNotifications.class).showWarning(
				"Not yet implemented");
	}

	public static UrlBuilder getBaseUrlBuilder() {
		UrlBuilder builder = new UrlBuilder();
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

	public static void fireHistoryToken(String token) {
		if (token == null) {
			return;
		}
		if (token.equals(History.getToken())) {
			History.fireCurrentHistoryState();
		}
		History.newItem(token);
	}

	public static EditContentViewWidgets editContentView(final Object model,
			final PermissibleActionListener pal, String caption,
			String messageHtml) {
		return popupContentView(model, pal, caption, messageHtml, true, true);
	}

	public static EditContentViewWidgets showContentView(final Object model,
			final PermissibleActionListener pal, String caption,
			String messageHtml) {
		return popupContentView(model, pal, caption, messageHtml, true, false);
	}

	public static EditContentViewWidgets makeContentView(final Object model,
			boolean editable) {
		return makeContentView(model, null, null, null, false, editable, false,
				true);
	}

	public static EditContentViewWidgets popupContentView(final Object model,
			final PermissibleActionListener pal, String caption,
			String messageHtml, final boolean hideOnClick, boolean editable) {
		return makeContentView(model, pal, caption, messageHtml, hideOnClick,
				editable, true, false);
	}

	private static EditContentViewWidgets makeContentView(final Object model,
			final PermissibleActionListener pal, String caption,
			String messageHtml, final boolean hideOnClick, boolean editable,
			boolean inDialog, boolean noButtons) {
		ContentViewFactory cvf = new ContentViewFactory();
		cvf.setNoCaption(true);
		cvf.setNoButtons(noButtons);
		cvf.setCancelButton(true);
		FlowPanel fp = new FlowPanel();
		final GlassDialogBox gdb = new GlassDialogBox();
		PermissibleActionListener closeWrapper = new PermissibleActionListener() {
			@Override
			public void vetoableAction(PermissibleActionEvent evt) {
				if (hideOnClick) {
					gdb.hide();
				}
				if (pal != null) {
					pal.vetoableAction(evt);
				}
			}
		};
		PaneWrapperWithObjects view = cvf.createBeanView(model, editable,
				inDialog ? closeWrapper : null, false, true);
		view.addStyleName("pwo-center-buttons");
		if (!editable && inDialog) {
			OkCancelPanel sp = new OkCancelPanel("OK", view, false);
			view.add(sp);
		}
		List<Binding> bindings = view.getBoundWidget().getBinding()
				.getChildren();
		for (Binding b : bindings) {
			RelativePopupValidationFeedback feedback = new RelativePopupValidationFeedback(
					RelativePopupValidationFeedback.BOTTOM,
					b.getLeft().feedback);
			feedback.setCss("withBkg");
			b.getLeft().feedback = feedback;
		}
		gdb.setText(caption);
		if (messageHtml != null) {
			HTML message = new HTML(messageHtml);
			message.setStyleName("bean-panel-message");
			fp.add(message);
		}
		fp.add(view);
		if (inDialog) {
			gdb.add(fp);
			gdb.center();
			gdb.show();
		}
		return new EditContentViewWidgets(view, inDialog ? gdb : null);
	}

	public static class EditContentViewWidgets {
		public PaneWrapperWithObjects wrapper;

		public GlassDialogBox gdb;

		public EditContentViewWidgets(PaneWrapperWithObjects wrapper,
				GlassDialogBox gdb) {
			this.wrapper = wrapper;
			this.gdb = gdb;
		}
	}

	public static String simpleInnerText(String innerHTML) {
		int idx = 0, idy = 0, x1, x2, min;
		StringBuilder result = new StringBuilder();
		while (true) {
			x1 = innerHTML.indexOf("<", idx);
			x2 = innerHTML.indexOf("&", idx);
			if (x1 == -1 && x2 == -1) {
				break;
			}
			min = x1 != -1 && (x2 == -1 || x1 < x2) ? x1 : x2;
			result.append(innerHTML.substring(idx, min));
			if (min == x1) {
				x2 = innerHTML.indexOf(">", min);
				if (x2 == -1) {
					// invalidish html, bail
					break;
				} else {
					idx = x2 + 1;
				}
			} else {
				x2 = innerHTML.indexOf(";", min);
				if (x2 == -1) {
					// invalidish html, bail
					break;
				} else {
					String minStr = innerHTML.substring(min + 1, x2);
					if (minStr.equals("amp")) {
						result.append("&");
					} else if (minStr.equals("lt")) {
						result.append("<");
					} else if (minStr.equals("gt")) {
						result.append(">");
					} else {
						try {
							int cc = Integer.parseInt(minStr);
							result.append((char) cc);
						} catch (NumberFormatException ne) {
							// ignore
						}
					}
					idx = x2 + 1;
				}
			}
		}
		result.append(innerHTML.substring(idx));
		return result.toString();
	}

	public static void refireHistoryTokenIfSame(String token) {
		if (token == null) {
			return;
		}
		if (token.equals(History.getToken())) {
			History.fireCurrentHistoryState();
		}
		// do nothing if we've moved on
	}

	public static native void invokeJsDebugger(Element e) /*-{
        var v = e;
        debugger;
	}-*/;

	public static String getHashIfSelfrefUrl(Element anchor) {
		String href = anchor.getAttribute("href");
		String selfHref = Window.Location.getHref();
		int idx = selfHref.indexOf("#");
		selfHref = idx == -1 ? selfHref : selfHref.substring(0, idx);
		if (href.startsWith(selfHref)) {
			href = href.substring(selfHref.length());
		}
		return href.startsWith("#") && href.length() > 1 ? href.substring(1)
				: null;
	}

	public static List<String> jsStringArrayAsStringList(
			JsArrayString arrayString) {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < arrayString.length(); i++) {
			result.add(arrayString.get(i));
		}
		return result;
	}

	public static <T extends JavaScriptObject> List<T> jsArrayToTypedArray(
			JsArray<T> typedArray) {
		List<T> result = new ArrayList<T>();
		for (int i = 0; i < typedArray.length(); i++) {
			result.add(typedArray.get(i));
		}
		return result;
	}

	public static <T extends JavaScriptObject> JsArray<T> toTypedJsArray(
			List<T> value) {
		JsArray<T> array = JavaScriptObject.createArray().cast();
		for (T t : value) {
			array.push(t);
		}
		return array;
	}
}
