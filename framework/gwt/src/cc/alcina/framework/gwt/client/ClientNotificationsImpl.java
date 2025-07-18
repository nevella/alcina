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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.client.widget.dialog.GlassDialogBox;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;

public class ClientNotificationsImpl implements ClientNotifications {
	private static final String LT_NOTIFY_COMPLETED_SAVE = "LT_NOTIFY_COMPLETED_SAVE";

	private DialogBox dialogBox;

	private HTML dialogHtml;

	private String logString = "";

	protected StandardDataImages images;

	private boolean dialogAnimationEnabled = true;

	private boolean logToSysOut;

	private Map<String, Long> metricStartTimes = new HashMap<String, Long>();

	Map<String, String> localisedMessages;

	private Set<String> enqueuedOncePerInstanceNotificationBodies = new LinkedHashSet<>();

	private TopicListener<String> logListener = message -> {
		log(message);
		System.out.println(message);
	};

	private List<ClientNotification> notificationQueue = new ArrayList<>();

	private PropertyChangeListener uiStateListener = null;

	NonCancellableRemoteDialog notifier = null;

	public ClientNotificationsImpl() {
		AlcinaTopics.logMessage.add(logListener);
	}

	@Override
	public void confirm(String msg, final OkCallback callback) {
		new OkCancelDialogBox("Confirmation", new Label(msg),
				new PermissibleActionListener() {
					@Override
					public void vetoableAction(PermissibleActionEvent evt) {
						if (evt.getAction() == OkAction.INSTANCE) {
							callback.ok();
						}
					}
				}).show();
	}

	protected native void consoleLog(String s) /*-{
    try {
      $wnd.console.log(s);
    } catch (e) {

    }
	}-*/;

	@Override
	public void enqueue(ClientNotification notification) {
		if (notification.isOncePerClientInstance()
				&& !enqueuedOncePerInstanceNotificationBodies
						.add(notification.getBody())) {
			return;
		}
		notificationQueue.add(notification);
		flushNotifications();
	}

	private void ensureImages() {
		if (images == null) {
			images = GWT.create(StandardDataImages.class);
		}
	}

	public void ensureLocalisedMessages() {
		if (localisedMessages == null) {
			localisedMessages = new HashMap<String, String>();
			localisedMessages.put(LT_NOTIFY_COMPLETED_SAVE,
					TextProvider.get().getUiObjectText(
							ClientNotificationsImpl.class,
							LT_NOTIFY_COMPLETED_SAVE,
							"Your offline work has been saved to the server"));
		}
	}

	private void flushNotifications() {
		if (!ClientState.get().isUiInitialised() && uiStateListener == null) {
			uiStateListener = evt -> flushNotifications();
			ClientState.get().addPropertyChangeListener("uiInitialised",
					uiStateListener);
			return;
		}
		notificationQueue.stream()
				.filter(notification -> ClientState.get().isUiInitialised()
						|| notification.isShowBeforeUiInitComplete())
				.findFirst().ifPresent(notification -> {
					notificationQueue.remove(notification);
					showDialog(notification.getCaption(), null,
							notification.getBody(),
							MessageType.fromNotificationLevel(
									notification.getLevel()),
							null);
					new Timer() {
						@Override
						public void run() {
							flushNotifications();
						}
					}.schedule(2000);
				});
	}

	@Override
	public String getLogString() {
		return this.logString;
	}

	@Override
	public ModalNotifier getModalNotifier(String message) {
		if (notifier == null || !notifier.isAttached()) {
			notifier = new NonCancellableRemoteDialog(message, null, false) {
				@Override
				protected boolean initialAnimationEnabled() {
					return false;
				}
			};
		}
		notifier.setText(message);
		return notifier;
	}

	protected String getStandardErrorText() {
		return "Sorry for the inconvenience, and we'll fix this problem as soon as possible."
				+ ""
				+ " If the problem recurs, please try refreshing your browser";
	}

	@Override
	public void hideDialog() {
		if (dialogBox != null) {
			dialogBox.hide();
		}
	}

	@Override
	public boolean isDialogAnimationEnabled() {
		return dialogAnimationEnabled;
	}

	public boolean isLogToSysOut() {
		return this.logToSysOut;
	}

	protected boolean isViewDetail() {
		return true;
	}

	@Override
	public void log(String s) {
		log(s, AlcinaTopics.LOG_CATEGORY_MESSAGE);
	}

	public void log(String s, String category) {
		if (logString.length() < 30000) {
			logString += DateStyle.DATE_TIME_MS.format(new Date()) + ": " + s
					+ "\n";
		}
		consoleLog(s);
		if (logToSysOut) {
			System.out.println(s);
		}
		AlcinaTopics.categorisedLogMessage.publish(new StringPair(category, s));
	}

	@Override
	public void metricLogEnd(String key) {
		if (metricStartTimes.containsKey(key)) {
			log(Ax.format("Metric: %s - %s ms", key,
					System.currentTimeMillis() - metricStartTimes.get(key)),
					AlcinaTopics.LOG_CATEGORY_METRIC);
			metricStartTimes.remove(key);
		}
	}

	@Override
	public void metricLogStart(String key) {
		metricStartTimes.put(key, System.currentTimeMillis());
	}

	@Override
	public void notifyOfCompletedSaveFromOffline() {
		ensureLocalisedMessages();
		Window.alert(localisedMessages.get(LT_NOTIFY_COMPLETED_SAVE));
	}

	@Override
	public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
		this.dialogAnimationEnabled = dialogAnimationEnabled;
	}

	public void setLogToSysOut(boolean logToSysOut) {
		this.logToSysOut = logToSysOut;
	}

	@Override
	public void showDevError(Throwable e) {
		MessageManager.get().icyMessage("GWT exception - " + e.getMessage());
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget, String msg,
			MessageType messageType, List<Button> extraButtons) {
		showDialog(captionHTML, captionWidget, msg, messageType, extraButtons,
				null);
	}

	@Override
	public void showDialog(String captionHTML, Widget captionWidget, String msg,
			MessageType messageType, List<Button> extraButtons,
			String containerStyle) {
		ensureImages();
		HorizontalAlignmentConstant align = messageType == MessageType.ERROR
				? HasHorizontalAlignment.ALIGN_LEFT
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
				: (messageType == MessageType.ERROR
						|| !CommonUtils.isNullOrEmpty(msg)) ? "medium"
								: "narrow";
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
		if (!CommonUtils.isNullOrEmpty(msg) && isViewDetail()) {
			Link nh = new Link("View detail");
			nh.addStyleName("pad-5");
			dialogHtml = new HTML("<span class='logboxpre'>"
					+ msg.replace("\n", "<br>") + "</span>", true);
			final ScrollPanel sp = new ScrollPanel(dialogHtml);
			sp.setStyleName("logbox");
			sp.setVisible(containerStyle.equals("wide"));
			nh.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					sp.setVisible(!sp.isVisible());
				}
			});
			if (LooseContext.getBoolean(
					ClientNotifications.CONTEXT_AUTOSHOW_DIALOG_DETAIL)) {
				sp.setVisible(true);
			}
			fp.add(nh);
			fp.add(sp);
		}
		ft.setWidget(0, 1, fp);
		cf.setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_MIDDLE);
		HorizontalPanel hp = new HorizontalPanel();
		hp.setSpacing(8);
		Button closeButton = new Button("Close");
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
			@Override
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});
		dialogBox.setWidget(ft);
		dialogBox.center();
		dialogBox.show();
		Scheduler.get().scheduleDeferred(() -> closeButton.setFocus(true));
	}

	@Override
	public void showError(String msg, Throwable throwable) {
		String exceptionTrace = throwable.toString();
		log("error: " + msg.replace("<br>", "\n") + "\n" + exceptionTrace);
		msg += CommonUtils.isNullOrEmpty(msg) ? "" : "<br><br>";
		msg += getStandardErrorText();
		msg = "<div class='errorOops'>Ooops - an error has occurred</div>"
				+ "<div class='errorSub'>" + msg + "</div>";
		if (!Permissions.isDeveloper()) {
			exceptionTrace = "";
		}
		showDialog(msg, null, exceptionTrace, MessageType.ERROR,
				new ArrayList<Button>());
	}

	@Override
	public void showError(Throwable caught) {
		this.showError("", caught);
	}

	@Override
	public void showLog() {
		showDialog(Ax.format("<div>Client log</div><hr>"
				+ "<div class='logboxpre' style='width:850px'>%s </div>",
				(logString + statsString()).replace("\n", "<br>")), null, null,
				MessageType.INFO, null, "wide");
		dialogBox.setModal(false);
		dialogBox.setAutoHideEnabled(true);
	}

	@Override
	public void showMessage(String msg) {
		showDialog("<div class='info'>" + msg + "</div>", null, null,
				MessageType.INFO, null);
	}

	@Override
	public void showMessage(Widget msg) {
		showDialog(null, msg, null, MessageType.INFO, null);
	}

	@Override
	public void showWarning(String msg) {
		showDialog("<div class='warning'>" + msg + "</div>", null, null,
				MessageType.WARN, null);
	}

	@Override
	public void showWarning(String msg, String detail) {
		showDialog("<div class='warning'>" + msg + "</div>", null, detail,
				MessageType.WARN, null);
	}

	public native String statsString() /*-{
    if (!($wnd.__stats)) {
      return "";
    }
    var result = "";
    var lastEvtGroup = -1;
    var lastMillis = 0;
    for ( var k in $wnd.__stats) {
      var stat = $wnd.__stats[k];
      var deltaStr = '';
      for ( var j in stat) {
        var v = stat[j];
        result += j + ": " + v + "  ";

      }

      var v = stat.evtGroup;
      if (lastEvtGroup == v) {
        if (lastMillis != 0) {
          result += "\ndelta - " + v + " - " + stat.type + ' - '
              + (stat.millis - lastMillis) + 'ms\n';
        }
        lastMillis = stat.millis;
      } else {
        lastMillis = 0;
        lastEvtGroup = v;
      }
      result += "\n\n";
    }
    return result;
	}-*/;

	public enum MessageType {
		INFO, WARN, ERROR;

		static MessageType fromNotificationLevel(Level level) {
			if (level == null) {
				return INFO;
			}
			switch (level) {
			case LOG:
				throw new UnsupportedOperationException();
			case INFO:
				return INFO;
			case EXCEPTION:
				return ERROR;
			case WARNING:
				return WARN;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
