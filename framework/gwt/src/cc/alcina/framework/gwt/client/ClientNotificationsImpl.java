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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;

public class ClientNotificationsImpl implements ClientNofications {
	private DialogBox dialogBox;

	private HTML dialogHtml;

	private String logString = "";

	protected static final StandardDataImages images = GWT
			.create(StandardDataImages.class);

	private boolean dialogAnimationEnabled = true;

	public void confirm(String msg, final OkCallback callback) {
		new OkCancelDialogBox("Confirmation", new Label(msg),
				new PermissibleActionListener() {
					public void vetoableAction(PermissibleActionEvent evt) {
						if (evt.getAction().getActionName()
								.equals(OkCancelDialogBox.OK_ACTION)) {
							callback.ok();
						}
					}
				}).show();
	}

	public String getLogString() {
		return this.logString;
	}

	public void hideDialog() {
		if (dialogBox != null) {
			dialogBox.hide();
		}
	}

	public boolean isDialogAnimationEnabled() {
		return dialogAnimationEnabled;
	}

	public void log(String s) {
		logString += CommonUtils.formatDate(new Date(),
				DateStyle.AU_DATE_TIME_MS) + ": " + s + "\n";
		consoleLog(s);
	}

	private native void consoleLog(String s) /*-{
		try {
			$wnd.console.log(s);
		} catch (e) {

		}
	}-*/;

	private Map<String, Long> metricStartTimes = new HashMap<String, Long>();

	public void metricLogEnd(String key) {
		if (metricStartTimes.containsKey(key)) {
			log(CommonUtils.format("Metric: %1 - %2 ms", key,
					System.currentTimeMillis() - metricStartTimes.get(key)));
			metricStartTimes.remove(key);
		}
	}

	public void metricLogStart(String key) {
		metricStartTimes.put(key, System.currentTimeMillis());
	}

	public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
		this.dialogAnimationEnabled = dialogAnimationEnabled;
	}

	public void showDialog(String captionHTML, Widget captionWidget,
			String msg, MessageType messageType, List<Button> extraButtons) {
		showDialog(captionHTML, captionWidget, msg, messageType, extraButtons,
				null);
	}

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
		if (captionHTML != null) {
			capWidget.setStyleName("caption");
		}
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

	public void showError(String msg, Throwable throwable) {
		msg += CommonUtils.isNullOrEmpty(msg) ? "" : "<br><br>";
		msg += getStandardErrorText();
		msg = "<div class='errorOops'>Ooops - an error has occurred</div>"
				+ "<div class='errorSub'>" + msg + "</div>";
		showDialog(msg, null, throwable.toString(), MessageType.ERROR,
				new ArrayList<Button>());
	}

	public void showError(Throwable caught) {
		this.showError("", caught);
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

	public void showMessage(String msg) {
		showDialog("<div class='info'>" + msg + "</div>", null, null,
				MessageType.INFO, null);
	}

	public void showMessage(Widget msg) {
		showDialog(null, msg, null, MessageType.INFO, null);
	}

	public void showWarning(String msg) {
		showDialog("<div class='warning'>" + msg + "</div>", null, null,
				MessageType.WARN, null);
	}

	public void showWarning(String msg, String detail) {
		showDialog("<div class='warning'>" + msg + "</div>", null, detail,
				MessageType.WARN, null);
	}

	protected String getStandardErrorText() {
		return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
				+ ""
				+ " If the problem recurs, please try refreshing your browser";
	}

	public enum MessageType {
		INFO, WARN, ERROR
	}
}
