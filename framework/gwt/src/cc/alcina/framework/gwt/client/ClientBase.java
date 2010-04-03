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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteAction;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionEvent.PermissibleActionListener;
import cc.alcina.framework.common.client.csobjects.LoginBean;
import cc.alcina.framework.common.client.csobjects.LoginResponseBean;
import cc.alcina.framework.common.client.csobjects.WebException;
import cc.alcina.framework.common.client.entity.GwtPersistableObject;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.spi.LogWriter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.ide.provider.PropertiesProvider;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.logic.StandardAsyncCallback;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public abstract class ClientBase implements EntryPoint,
		UncaughtExceptionHandler, LogWriter {
	private static final String HEAD = "head";

	private static final String CSS_TEXT_PROPERTY = "cssText";

	public abstract ClientInstance getClientInstance();

	/**
	 * This is the entry point method.
	 */
	private DialogBox dialogBox;

	private HTML dialogHtml;

	private PropertyChangeListener cssPropertyListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			updateDeveloperCss();
		}
	};

	private String logString = "";

	public String getLogString() {
		return this.logString;
	}

	public abstract CommonRemoteServiceAsync getCommonRemoteService();

	public abstract void handleLoggedIn(LoginResponseBean lrb);

	public void persist(GwtPersistableObject gpo) {
		persist(gpo, new StandardAsyncCallback());
	}

	@SuppressWarnings("unchecked")
	public void persist(GwtPersistableObject gpo, AsyncCallback callback) {
		getCommonRemoteService().persist(gpo, callback);
	}

	protected void addCssListeners(GeneralProperties props) {
		props.addPropertyChangeListener(
				GeneralProperties.PROPERTY_PERSISTENT_CSS, cssPropertyListener);
		props.addPropertyChangeListener(
				GeneralProperties.PROPERTY_TRANSIENT_CSS, cssPropertyListener);
	}

	private void addHidden(Panel p, String key, String value) {
		p.add(new Hidden(key, value));
	}

	public native void invokeJsDebugger() /*-{
		debugger;
	}-*/;

	public void log(String s) {
		logString += CommonUtils.formatDate(new Date(),
				DateStyle.AU_DATE_TIME_MS)
				+ ": " + s + "\n";
	}

	public static boolean maybeOffline(Throwable t) {
		if (t instanceof WrappedRuntimeException) {
			t = t.getCause();
		}
		if (t instanceof StatusCodeException) {
			StatusCodeException sce = (StatusCodeException) t;
			ClientLayerLocator.get().clientBase().log(
					"** Status code exception: " + sce.getStatusCode());
			if ((!GWT.isScript() && sce.getStatusCode() == 500)
					|| sce.getStatusCode() == 0) {
				return true;
			}
			// DNS error in Africa
			if (t.toString().contains("was not able to resolve the hostname")) {
				return true;
			}
		}
		return false;
	}

	public void onUncaughtException(Throwable e) {
		// TODO - 3.02
		GWT.log("Uncaught exception escaped", e);
		if (GWT.isScript()) {
			showError(e);
		}
	}

	protected void removeCssListeners(GeneralProperties props) {
		props.removePropertyChangeListener(
				GeneralProperties.PROPERTY_PERSISTENT_CSS, cssPropertyListener);
		props.removePropertyChangeListener(
				GeneralProperties.PROPERTY_TRANSIENT_CSS, cssPropertyListener);
	}

	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	public void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons) {
		showDialog(captionHTML, captionWidget, msg, messageType, extraButtons,
				null);
	}

	private boolean dialogAnimationEnabled = true;

	public void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons,
			String containerStyle) {
		HorizontalAlignmentConstant align = messageType == MessageType.ERROR ? HasHorizontalAlignment.ALIGN_LEFT
				: HasHorizontalAlignment.ALIGN_CENTER;
		if (dialogBox != null) {
			dialogBox.hide();
		}
		String title = CommonUtils.friendlyConstant(messageType);
		dialogBox = new GlassDialogBox();
		dialogBox.setAnimationEnabled(dialogAnimationEnabled);
		AbstractImagePrototype aip = null;
		String text = "";
		switch (messageType) {
		case INFO:
			aip = AbstractImagePrototype.create(images.info());
			text = "Information";
			break;
		case WARN:
			aip = AbstractImagePrototype.create(images.warning());
			text = "Warning";
			break;
		case ERROR:
			aip = AbstractImagePrototype.create(images.error());
			text = "Problem notification";
			break;
		}
		dialogBox.setText(text);
		FlexTable ft = new FlexTable();
		containerStyle = containerStyle != null ? containerStyle
				: (messageType == MessageType.ERROR || !CommonUtils
						.isNullOrEmpty(msg)) ? "medium" : "narrow";
		ft.setCellSpacing(4);
		ft.setStyleName("alcina-Notification");
		ft.addStyleName(containerStyle);
		FlexCellFormatter cf = (FlexCellFormatter) ft.getCellFormatter();
		ft.setWidget(0, 0, aip.createImage());
		cf.setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
		cf.setWidth(0, 0, "40px");
		FlowPanel fp = new FlowPanel();
		fp.setStyleName("text");
		Widget capWidget = captionHTML != null ? new HTML(captionHTML)
				: captionWidget;
		capWidget.setStyleName("caption");
		fp.add(capWidget);
		if (!CommonUtils.isNullOrEmpty(msg)) {
			Link nh = new Link("View detail");
			nh.addStyleName("pad-5");
			dialogHtml = new HTML("<span class='logboxpre'>"
					+ msg.replace("\n", "<br>") + "</span>", true);
			final ScrollPanel sp = new ScrollPanel(dialogHtml);
			sp.setStyleName("logbox");
			sp.setVisible(containerStyle.equals("wide"));
			nh.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					sp.setVisible(!sp.isVisible());
				}
			});
			fp.add(nh);
			fp.add(sp);
		}
		ft.setWidget(0, 1, fp);
		cf.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(8);
		Button closeButton = new Button("close");
		hp.add(closeButton);
		if (extraButtons != null) {
			for (Button b : extraButtons) {
				hp.add(b);
			}
		}
		ft.setWidget(1, 0, hp);
		cf.setColSpan(1, 0, 2);
		cf.setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_CENTER);
		closeButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		dialogBox.setWidget(ft);
		dialogBox.center();
		dialogBox.show();
		closeButton.setFocus(true);
	}

	public void showError(Throwable caught) {
		this.showError("", caught);
	}

	public void hideDialog() {
		if (dialogBox != null) {
			dialogBox.hide();
		}
	}

	public void showError(String msg, Throwable throwable) {
		msg += CommonUtils.isNullOrEmpty(msg) ? "" : "<br><br>";
		msg += getStandardErrorText();
		msg = "<div class='errorOops'>Ooops - an error has occurred</div>"
				+ "<div class='errorSub'>" + msg + "</div>";
		showDialog(msg, null, throwable.toString(), MessageType.ERROR,
				new ArrayList<Button>());
	}

	protected String getStandardErrorText() {
		return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
				+ ""
				+ " If the problem recurs, please try refreshing your browser";
	}

	public void showLog() {
		Button b = new Button("clear log", new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				logString = "";
				dialogHtml.setHTML("");
			}
		});
		Button c = new Button("copy to clipboard", new ClickHandler() {
			public void onClick(ClickEvent event) {
				Widget sender = (Widget) event.getSource();
				WidgetUtils.copyTextToClipboard(logString);
			}
		});
		showDialog("Client log - performance metrics", null, logString,
				MessageType.INFO, Arrays.asList(new Button[] { b, c }), "wide");
	}

	public void showWarning(String msg) {
		showDialog("<div class='warning'>" + msg + "</div>", null, null,
				MessageType.WARN, null);
	}

	public void showWarning(String msg, String detail) {
		showDialog("<div class='warning'>" + msg + "</div>", null, detail,
				MessageType.WARN, null);
	}

	public void showMessage(String msg) {
		showDialog("<div class='info'>" + msg + "</div>", null, null,
				MessageType.INFO, null);
	}

	public void showMessage(Widget msg) {
		showDialog(null, msg, null, MessageType.INFO, null);
	}

	public void submitForm(Map<String, String> params, String url) {
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

	private Element styleElement;

	public Element updateCss(Element styleElement, String css) {
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
						showMessage("Sorry, this action is not supported on some versions of Internet Explorer");
					}
				}
			}
		}
		return styleElement;
	}

	public void updateDeveloperCss() {
		String css = PropertiesProvider.getGeneralProperties()
				.getPersistentCss()
				+ PropertiesProvider.getGeneralProperties().getTransientCss();
		this.styleElement = updateCss(styleElement, css);
	}

	public void hello(AsyncCallback callback) {
		getCommonRemoteService().hello(callback);
	}

	public void login(LoginBean loginBean, AsyncCallback callback) {
		getCommonRemoteService().login(loginBean, callback);
	}

	public void logout(AsyncCallback callback) {
		getCommonRemoteService().logout(callback);
	}

	public void transform(DomainTransformRequest request, AsyncCallback callback) {
		getCommonRemoteService().transform(request, callback);
	}

	public void getLogsForAction(RemoteAction action, int count,
			AsyncCallback<List<ActionLogItem>> callback) {
		getCommonRemoteService().getLogsForAction(action,
				Integer.valueOf(count), callback);
	}

	public void performAction(RemoteAction action, AsyncCallback<Long> callback) {
		getCommonRemoteService().performAction(action, callback);
	}

	public <T extends HasIdAndLocalId> void getItemById(String className,
			Long id, AsyncCallback<T> callback) {
		getCommonRemoteService().getItemById(className, id, callback);
	}

	public void confirm(String msg, final OkCallback callback) {
		new OkCancelDialogBox("Confirmation", new Label(msg),
				new PermissibleActionEvent.PermissibleActionListener() {
					public void vetoableAction(PermissibleActionEvent evt) {
						if (evt.getAction().getActionName().equals(
								OkCancelDialogBox.OK_ACTION)) {
							callback.ok();
						}
					}
				}).show();
	}

	public void notImplemented() {
		this.showWarning("Not yet implemented");
	}

	public enum MessageType {
		INFO, WARN, ERROR
	}

	private Map<String, Long> metricStartTimes = new HashMap<String, Long>();

	public void metricLogStart(String key) {
		metricStartTimes.put(key, System.currentTimeMillis());
	}

	public void metricLogEnd(String key) {
		log(CommonUtils.format("Metric: %1 - %2 ms", key, System
				.currentTimeMillis()
				- metricStartTimes.get(key)));
	}

	public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
		this.dialogAnimationEnabled = dialogAnimationEnabled;
	}

	public boolean isDialogAnimationEnabled() {
		return dialogAnimationEnabled;
	}

	public String extraInfoForExceptionText() {
		return "\n\nUser agent: " + BrowserMod.getUserAgent()
				+ "\n\nHistory token: " + History.getToken();
	}

	protected Throwable possiblyWrapJavascriptException(Throwable e) {
			if (e instanceof JavaScriptException) {
				JavaScriptException je = (JavaScriptException) e;
				String errorText = je.getMessage();
	//			if (BrowserMod.isChrome()){
	//				errorText+="\n\nStacktrace: "+je.get
	//			}
				errorText += extraInfoForExceptionText();
				e = new WebException("(Wrapped javascript exception) : "
						+ errorText);
			}
			return e;
		}
}
