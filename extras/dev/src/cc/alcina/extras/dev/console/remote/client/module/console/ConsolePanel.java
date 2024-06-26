package cc.alcina.extras.dev.console.remote.client.module.console;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;

import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout;
import cc.alcina.extras.dev.console.remote.client.RemoteConsoleLayout.RemoteConsoleLayoutMessage;
import cc.alcina.extras.dev.console.remote.client.common.logic.RemoteConsoleClientUtils;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleConsoleChanges;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest.RemoteConsoleRequestType;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleResponse;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.lux.LuxMainPanel;
import cc.alcina.framework.gwt.client.lux.LuxMainPanelBuilder;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class ConsolePanel extends Composite {
	private FlowPanel fp;

	@SuppressWarnings("unused")
	private ConsoleActivity consoleActivity;

	private OutputPanel outputPanel;

	private CommandBarPanel commandBarPanel;

	public ConsolePanel(ConsoleActivity consoleActivity) {
		this.consoleActivity = consoleActivity;
		this.fp = new FlowPanel();
		initWidget(fp);
		render();
		getRecords();
	}

	void arrowDown() {
		RemoteConsoleRequest request = RemoteConsoleRequest.create();
		request.setType(RemoteConsoleRequestType.ARROW_DOWN);
		try {
			RemoteConsoleClientUtils.submitRequest(request, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void arrowUp() {
		RemoteConsoleRequest request = RemoteConsoleRequest.create();
		request.setType(RemoteConsoleRequestType.ARROW_UP);
		try {
			RemoteConsoleClientUtils.submitRequest(request, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getRecords() {
		RemoteConsoleRequest request = RemoteConsoleRequest.create();
		request.setType(RemoteConsoleRequestType.GET_RECORDS);
		RemoteConsoleClientUtils.submitRequest(request, this::handleRecords);
	}

	void handleRecords(RemoteConsoleResponse response) {
		if (response == null) {
			Ax.out("missing response [down?]");
			new Timer() {
				@Override
				public void run() {
					getRecords();
				}
			}.schedule(2000);
			return;
		}
		RemoteConsoleConsoleChanges changes = response.getChanges();
		if (changes.isClearOutput()) {
			outputPanel.clearContents();
		}
		outputPanel.addHtml(changes.getOutputHtml());
		if (Ax.notBlank(changes.getCommandLine())) {
			commandBarPanel.setCommandText(changes.getCommandLine());
		}
		getRecords();
	}

	private void render() {
		fp.clear();
		LuxMainPanelBuilder builder = new LuxMainPanelBuilder();
		LuxMainPanel mainPanel = builder.build();
		ConsoleStyles.CONSOLE.addTo(mainPanel);
		outputPanel = new OutputPanel();
		commandBarPanel = new CommandBarPanel();
		mainPanel.add(outputPanel);
		mainPanel.add(commandBarPanel);
		fp.add(mainPanel);
	}

	void submitCommand(String string) {
		RemoteConsoleRequest request = RemoteConsoleRequest.create();
		request.setCommandString(string);
		request.setType(RemoteConsoleRequestType.DO_COMMAND);
		try {
			RemoteConsoleClientUtils.submitRequest(request, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	class CommandBarPanel extends Composite {
		private FlowPanel fp;

		private TextBox box;

		public CommandBarPanel() {
			this.fp = new FlowPanel();
			initWidget(fp);
			render();
		}

		void focus() {
			Scheduler.get().scheduleDeferred(() -> box.setFocus(true));
		}

		@Override
		protected void onAttach() {
			super.onAttach();
			box.setFocus(true);
			RemoteConsoleLayout.get().topicLayoutMessage.add(m -> {
				if (m == RemoteConsoleLayoutMessage.FOCUS_COMMAND_BAR
						&& WidgetUtils.isVisibleAncestorChain(this)) {
					box.setFocus(true);
				}
			});
		}

		private void render() {
			fp.clear();
			ConsoleStyles.COMMAND_BAR.set(this);
			box = new TextBox();
			box.getElement().setAttribute("autocomplete", "off");
			fp.add(box);
			box.addKeyDownHandler(event -> {
				int keyCode = event.getNativeKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					Event.getCurrentEvent().preventDefault();
					ConsolePanel.this.submitCommand(box.getText());
					box.setText("");
				} else if (keyCode == KeyCodes.KEY_DOWN) {
					Event.getCurrentEvent().preventDefault();
					ConsolePanel.this.arrowDown();
				} else if (keyCode == KeyCodes.KEY_UP) {
					Event.getCurrentEvent().preventDefault();
					ConsolePanel.this.arrowUp();
				} else if (keyCode == (int) 'K'
						&& event.getNativeEvent().getMetaKey()) {
					outputPanel.clearContents();
				}
			});
		}

		public void setCommandText(String commandLine) {
			box.setText(commandLine);
			focus();
		}
	}

	class OutputPanel extends Composite {
		private FlowPanel fp;

		private FlowPanel inner;

		private ScrollPanel scrollPanel;

		int htmlCharCount = 0;

		public OutputPanel() {
			this.fp = new FlowPanel();
			initWidget(fp);
			render();
		}

		public void addHtml(String outputHtml) {
			if (outputHtml.length() > 200000) {
				clearContents();
				inner.add(new HTML("...truncated <br>"));
				outputHtml = outputHtml.substring(outputHtml.length() - 200000);
				int idx = outputHtml.indexOf("<");
				if (idx != -1) {
					outputHtml = outputHtml.substring(idx);
				}
				idx = outputHtml.indexOf("</");
				if (idx == 0) {
					idx = outputHtml.indexOf("<", 1);
					outputHtml = outputHtml.substring(idx);
				}
			}
			if (htmlCharCount > 200000) {
				clearContents();
				htmlCharCount = 0;
				inner.add(new HTML("...truncated <br>"));
			}
			inner.add(new HTML(outputHtml));
			htmlCharCount += outputHtml.length();
			Scheduler.get()
					.scheduleDeferred(() -> scrollPanel.scrollToBottom());
			commandBarPanel.focus();
		}

		public void clearContents() {
			inner.clear();
		}

		private void render() {
			ConsoleStyles.OUTPUT.set(this);
			fp.clear();
			inner = new FlowPanel();
			ConsoleStyles.OUTPUT_INNER.set(inner);
			scrollPanel = new ScrollPanel(inner);
			ConsoleStyles.OUTPUT_SCROLL.set(scrollPanel);
			fp.add(scrollPanel);
		}
	}
}
