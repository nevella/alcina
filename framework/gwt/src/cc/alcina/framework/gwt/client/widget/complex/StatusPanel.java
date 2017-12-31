package cc.alcina.framework.gwt.client.widget.complex;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientBase;
import cc.alcina.framework.gwt.client.LayoutManagerBase;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;

public class StatusPanel extends Composite {
	public static final String RUNNING_TEMPLATE = "<div>%s...<br><br></div><div class='loading2'></div>";

	public static final String WAIT_LINE_TEMPLATE = "<div><img src='/img/wait.gif'>&#160;%s ...</div>";

	public static StatusPanel current;

	public static void showMessageOrAlert(String message) {
		if (current != null) {
			current.setContent(message);
		} else {
			Window.alert(message);
		}
	}

	private HTML content;

	private HandlerRegistration nativePreviewHandlerRegistration;

	private boolean modal;

	private FlowPanel fp;

	private FlowPanel progressPanel;

	private FlowPanel progressInner;

	private String runningTemplate = RUNNING_TEMPLATE;

	private String problemStyleName = "problemito";

	List<StatusPanelModalNotifier> notifiers = new ArrayList<StatusPanel.StatusPanelModalNotifier>();

	List<StatusPanelModalNotifier> logNotifiers = new ArrayList<StatusPanel.StatusPanelModalNotifier>();

	private boolean keepNotifiersAsLog;

	boolean inresize = false;

	public StatusPanel() {
		fp = new FlowPanel();
		content = new HTML();
		fp.add(content);
		progressPanel = new FlowPanel();
		fp.add(progressPanel);
		progressPanel.setStyleName("progress");
		progressInner = new FlowPanel();
		progressPanel.add(progressInner);
		progressPanel.setVisible(false);
		initWidget(fp);
		setStyleName("status-panel");
		setVisible(false);
	}

	public StatusPanelModalNotifier addModalNotifier() {
		return new StatusPanelModalNotifier(this);
	}

	public void addNotifier(StatusPanelModalNotifier notifier) {
		if (!notifiers.contains(notifier)) {
			notifiers.add(notifier);
			notifiersChanged();
		}
	}

	public void clear() {
		for (StatusPanelModalNotifier notifier : new ArrayList<StatusPanelModalNotifier>(
				notifiers)) {
			notifier.modalOff();
		}
		notifiers.clear();
		logNotifiers.clear();
		content.setHTML("");
	}

	public void ensureCurrent() {
		if (isAttached()) {
			updateHandlers(true);
			adoptNotifiersFromCurrent();
			current = this;
			notifiersChanged();
		}
	}

	public void ensureModalOff() {
		updateHandlers(false);
	}

	public String getProblemStyleName() {
		return this.problemStyleName;
	}

	public String getRunningTemplate() {
		return this.runningTemplate;
	}

	public boolean isKeepNotifiersAsLog() {
		return this.keepNotifiersAsLog;
	}

	public boolean isModal() {
		return this.modal;
	}

	public void notifiersChanged() {
		modal = !notifiers.isEmpty();
		String logHtml = "";
		if (!logNotifiers.isEmpty()) {
			for (StatusPanelModalNotifier notifier : logNotifiers) {
				if (!logHtml.isEmpty()) {
					logHtml += "<br>";
				}
				logHtml += notifier.message;
			}
		}
		if (notifiers.isEmpty()) {
			modal = false;
			setContent(logHtml, false);
		} else {
			modal = true;
			String html = "";
			for (StatusPanelModalNotifier notifier : notifiers) {
				if (!html.isEmpty()) {
					html += "<br>";
				}
				html += notifier.message;
			}
			setRunning(html, logHtml);
		}
	}

	public void removeNotifier(StatusPanelModalNotifier notifier) {
		if (notifiers.remove(notifier) && keepNotifiersAsLog) {
			logNotifiers.add(notifier);
		}
		notifiersChanged();
	}

	public void setContent(String html) {
		setContent(html, true);
	}

	public void setKeepNotifiersAsLog(boolean keepNotifiersAsLog) {
		this.keepNotifiersAsLog = keepNotifiersAsLog;
	}

	public void setModal(boolean modal) {
		this.modal = modal;
	}

	public void setProblemStyleName(String problemStyleName) {
		this.problemStyleName = problemStyleName;
	}

	public void setProgress(double progress) {
		progressPanel.setVisible(true);
		int w = progressPanel.getOffsetWidth();
		int h = progressPanel.getOffsetHeight();
		progressInner.setPixelSize((int) Math.round(w * progress), h - 2);
	}

	public void setRunning(String html) {
		setRunning(html, "");
		setVisible(html != null);
	}

	public void setRunning(String runningHtml, String preRunningHtml) {
		setVisible(runningHtml != null);
		String pre = CommonUtils.isNotNullOrEmpty(preRunningHtml)
				? preRunningHtml + "<br>" : "";
		String running = CommonUtils.formatJ(runningTemplate, runningHtml);
		setContent(pre + running);
	}

	public void setRunningTemplate(String runningTemplate) {
		this.runningTemplate = runningTemplate;
	}

	public void setShowingProblem(boolean b) {
		setStyleName(problemStyleName, b);
	}

	private void adoptNotifiersFromCurrent() {
		if (current != null && current != this) {
			for (StatusPanelModalNotifier notifier : current.logNotifiers) {
				notifier.panel = this;
				logNotifiers.add(notifier);
			}
			for (StatusPanelModalNotifier notifier : current.notifiers) {
				notifier.panel = this;
				notifiers.add(notifier);
			}
		}
	}

	private void setContent(String html, boolean makeCurrent) {
		setVisible(CommonUtils.isNotNullOrEmpty(html));
		if (current != this && makeCurrent) {
			if (current != null) {
				current.updateHandlers(false);
			}
			adoptNotifiersFromCurrent();
			current = this;
		}
		content.setHTML(html);
		setShowingProblem(false);
		if (!inresize
				&& Registry.impl(LayoutManagerBase.class).isDisplayInitialised()
				&& Registry.impl(ClientBase.class).isUsesRootLayoutPanel()) {
			inresize = true;
			RootLayoutPanel.get().onResize();
			inresize = false;
		}
	}

	private void updateHandlers(boolean show) {
		// Remove any existing handlers.
		if (nativePreviewHandlerRegistration != null) {
			nativePreviewHandlerRegistration.removeHandler();
			nativePreviewHandlerRegistration = null;
		}
		// Create handlers if showing.
		if (show) {
			nativePreviewHandlerRegistration = Event
					.addNativePreviewHandler(new NativePreviewHandler() {
						public void
								onPreviewNativeEvent(NativePreviewEvent event) {
							previewNativeEvent(event);
						}
					});
		}
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		if (current == null) {
			current = this;
		} else {
		}
		updateHandlers(true);
	}

	@Override
	protected void onDetach() {
		updateHandlers(false);
		if (current == this) {
			current = null;
		}
		super.onDetach();
	}

	protected void previewNativeEvent(NativePreviewEvent event) {
		// if (modal) {
		// event.cancel();
		// return;
		// }
	}

	public static class StatusPanelModalNotifier implements ModalNotifier {
		StatusPanel panel;

		private String message;

		public StatusPanelModalNotifier(StatusPanel panel) {
			this.panel = panel;
		}

		@Override
		public void modalOff() {
			panel.removeNotifier(this);
		}

		@Override
		public void modalOn() {
			panel.addNotifier(this);
		}

		@Override
		public void setMasking(boolean masking) {
			// noop
		}

		public void setMessage(String message) {
			this.message = message;
		}

		@Override
		public void setProgress(double progress) {
			panel.setProgress(progress);
		}

		@Override
		public void setStatus(String status) {
			this.message = status;
			panel.notifiersChanged();
		}
	}
}