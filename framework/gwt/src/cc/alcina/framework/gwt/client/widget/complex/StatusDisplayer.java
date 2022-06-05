package cc.alcina.framework.gwt.client.widget.complex;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

public class StatusDisplayer {
	private static final int FADER_DURATION = 3000;

	private MouseOverHandler overHandler = new MouseOverHandler() {
		@Override
		public void onMouseOver(MouseOverEvent event) {
			if (statusTuple.fader != null) {
				statusTuple.fader.cancel();
			}
		}
	};

	private FaderTuple appTuple;

	private FaderTuple exceptionTuple;

	FaderTuple statusTuple;

	FaderTuple centerTuple;

	boolean detached = false;

	public StatusDisplayer() {
	}

	public void attach() {
		Label statusLabel = new Label();
		statusLabel.addMouseOverHandler(overHandler);
		statusTuple = new FaderTuple(statusLabel, "alcina-Status");
		Label messageLabel = new Label();
		appTuple = new FaderTuple(messageLabel, "alcina-Status app-Status");
		Label centerLabel = new Label();
		centerTuple = new FaderTuple(centerLabel, "alcina-Status-Center");
		HTML exceptionLabel = new HTML();
		exceptionTuple = new FaderTuple(exceptionLabel,
				"alcina-Status-Exception");
		CallManager.topicCallMade
				.add(message -> showMessage(message, Channel.CALL_MADE));
		MessageManager.topicMessagePublished.add(
				message -> showMessage(message, Channel.MESSAGE_PUBLISHED));
		MessageManager.topicAppMessagePublished.add(
				message -> showMessage(message, Channel.APP_MESSAGE_PUBLISHED));
		MessageManager.topicIcyMessagePublished.add(
				message -> showMessage(message, Channel.ICY_MESSAGE_PUBLISHED));
		MessageManager.topicCenterMessagePublished
				.add(message -> showMessage(message,
						Channel.CENTER_MESSAGE_PUBLISHED));
		MessageManager.topicIcyCenterMessagePublished
				.add(message -> showMessage(message,
						Channel.ICY_CENTER_MESSAGE_PUBLISHED));
		MessageManager.topicExceptionMessagePublished
				.add(message -> showMessage(message,
						Channel.EXCEPTION_MESSAGE_PUBLISHED));
		RootPanel.get().add(appTuple.holder);
		RootPanel.get().add(statusTuple.holder);
		RootPanel.get().add(centerTuple.holder);
		RootPanel.get().add(exceptionTuple.holder);
	}

	public void detach() {
		detached = true;
	}

	public void removeWidget() {
		appTuple.label.removeFromParent();
		statusTuple.label.removeFromParent();
		centerTuple.label.removeFromParent();
		exceptionTuple.label.removeFromParent();
	}

	private void showMessage(String message, Channel channel) {
		if (detached) {
			return;
		}
		boolean center = false;
		int duration = FADER_DURATION;
		FaderTuple ft = statusTuple;
		boolean withFade = true;
		if (channel == Channel.CENTER_MESSAGE_PUBLISHED
				|| channel == Channel.ICY_CENTER_MESSAGE_PUBLISHED) {
			if (channel == Channel.ICY_CENTER_MESSAGE_PUBLISHED) {
				duration = (int) TimeConstants.ONE_MINUTE_MS;
			}
			center = true;
			ft = centerTuple;
		} else if (channel == Channel.CALL_MADE) {
			withFade = false;
		} else if (channel == Channel.APP_MESSAGE_PUBLISHED) {
			withFade = false;
			ft = appTuple;
		} else if (channel == Channel.EXCEPTION_MESSAGE_PUBLISHED) {
			duration = 8000;
			ft = exceptionTuple;
		}
		Label label = ft.label;
		FaderAnimation fader = ft.fader;
		label.setStyleName("");
		if (fader != null) {
			fader.cancel();
		}
		SimplePanelWClick holder = ft.holder;
		String cssClassName = channel.getCssClassName();
		if (Ax.notBlank(cssClassName)) {
			label.addStyleName(cssClassName);
			holder.addStyleName(cssClassName);
		}
		WidgetUtils.setOpacity(holder, 0);
		message = CommonUtils.nullToEmpty(message);
		if (label instanceof HTML) {
			((HTML) label).setHTML(message);
		} else {
			label.setText(message);
		}
		holder.setVisible(!message.isEmpty());
		if (message.isEmpty()) {
			return;
		}
		if (center) {
			int w = holder.getOffsetWidth();
			int cw = Window.getClientWidth();
			holder.getElement().getStyle().setLeft((cw - w) / 2, Unit.PX);
		}
		WidgetUtils.setOpacity(holder, 100);
		if (withFade) {
			fader = new FaderAnimation(holder);
			ft.fader = fader;
			fader.run(duration);
		}
	}

	private class FaderAnimation extends Animation {
		private final SimplePanel holder;

		double preFade = 0.7;

		public FaderAnimation(SimplePanel holder) {
			this.holder = holder;
		}

		@Override
		protected void onComplete() {
			holder.setVisible(false);
		}

		@Override
		protected void onUpdate(double progress) {
			if (progress > preFade) {
				int opacityPercent = (int) (100
						* (1 - (progress - preFade) / (1 - preFade)));
				WidgetUtils.setOpacity(holder, opacityPercent);
			}
		}
	}

	enum Channel {
		CALL_MADE, CENTER_MESSAGE_PUBLISHED, MESSAGE_PUBLISHED,
		ICY_MESSAGE_PUBLISHED, ICY_CENTER_MESSAGE_PUBLISHED,
		EXCEPTION_MESSAGE_PUBLISHED, APP_MESSAGE_PUBLISHED;

		String getCssClassName() {
			switch (this) {
			case ICY_MESSAGE_PUBLISHED:
				return "icy";
			case ICY_CENTER_MESSAGE_PUBLISHED:
				return "icy-center";
			case CENTER_MESSAGE_PUBLISHED:
				return "sd-center-notification";
			default:
				return null;
			}
		}
	}

	class FaderTuple {
		final String defaultStyle;

		private SimplePanelWClick holder;

		FaderAnimation fader;

		Label label;

		public FaderTuple(Label label, String defaultStyle) {
			this.label = label;
			this.holder = new SimplePanelWClick(label);
			holder.setStyleName("alcina-Status-Holder");
			if (defaultStyle != null) {
				holder.addStyleName(defaultStyle);
			}
			holder.setVisible(false);
			this.defaultStyle = defaultStyle;
		}
	}

	static class SimplePanelWClick extends SimplePanel {
		private ClickHandler hideHandler = new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				setVisible(false);
			}
		};

		public SimplePanelWClick(Label label) {
			super(label);
			addDomHandler(hideHandler, ClickEvent.getType());
		}

		@Override
		protected void onDetach() {
			super.onDetach();
		}
	}
}
