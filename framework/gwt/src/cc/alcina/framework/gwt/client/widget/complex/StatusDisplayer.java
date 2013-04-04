package cc.alcina.framework.gwt.client.widget.complex;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;

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

	private TopicListener topicListener = new TopicListener<String>() {
		@Override
		public void topicPublished(String key, String message) {
			showMessage(message, key);
		}
	};

	private FaderTuple appTuple;

	private FaderTuple exceptionTuple;

	public StatusDisplayer() {
		stylePrefixes = new StringMap();
		stylePrefixes.put(MessageManager.TOPIC_ICY_MESSAGE_PUBLISHED, "icy");
		stylePrefixes.put(MessageManager.TOPIC_CENTER_MESSAGE_PUBLISHED,
				"sd-center-notification");
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
		GlobalTopicPublisher.get().addTopicListener(
				CallManager.TOPIC_CALL_MADE, topicListener);
		GlobalTopicPublisher.get().addTopicListener(
				MessageManager.TOPIC_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().addTopicListener(
				MessageManager.TOPIC_APP_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().addTopicListener(
				MessageManager.TOPIC_ICY_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().addTopicListener(
				MessageManager.TOPIC_CENTER_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get()
				.addTopicListener(
						MessageManager.TOPIC_EXCEPTION_MESSAGE_PUBLISHED,
						topicListener);
		RootPanel.get().add(appTuple.holder);
		RootPanel.get().add(statusTuple.holder);
		RootPanel.get().add(centerTuple.holder);
		RootPanel.get().add(exceptionTuple.holder);
	}

	public void detach() {
		GlobalTopicPublisher.get().removeTopicListener(
				CallManager.TOPIC_CALL_MADE, topicListener);
		GlobalTopicPublisher.get().removeTopicListener(
				MessageManager.TOPIC_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().removeTopicListener(
				MessageManager.TOPIC_APP_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().removeTopicListener(
				MessageManager.TOPIC_ICY_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get().removeTopicListener(
				MessageManager.TOPIC_CENTER_MESSAGE_PUBLISHED, topicListener);
		GlobalTopicPublisher.get()
				.removeTopicListener(
						MessageManager.TOPIC_EXCEPTION_MESSAGE_PUBLISHED,
						topicListener);
	}

	private StringMap stylePrefixes;

	class FaderTuple {
		final String defaultStyle;

		private SimplePanel holder;

		public FaderTuple(Label label, String defaultStyle) {
			this.label = label;
			this.holder = new SimplePanel(label);
			holder.setStyleName("alcina-Status-Holder");
			if (defaultStyle != null) {
				holder.addStyleName(defaultStyle);
			}
			holder.setVisible(false);
			this.defaultStyle = defaultStyle;
		}

		FaderAnimation fader;

		Label label;
	}

	FaderTuple statusTuple;

	FaderTuple centerTuple;

	private void showMessage(String message, String channel) {
		boolean center = false;
		int duration=FADER_DURATION;
		FaderTuple ft = statusTuple;
		boolean withFade = true;
		if (channel == MessageManager.TOPIC_CENTER_MESSAGE_PUBLISHED) {
			center = true;
			ft = centerTuple;
		} else if (channel == CallManager.TOPIC_CALL_MADE) {
			withFade = false;
		} else if (channel == MessageManager.TOPIC_APP_MESSAGE_PUBLISHED) {
			withFade = false;
			ft = appTuple;
		} else if (channel == MessageManager.TOPIC_EXCEPTION_MESSAGE_PUBLISHED) {
			duration=8000;
			ft = exceptionTuple;
		}
		Label label = ft.label;
		FaderAnimation fader = ft.fader;
		label.setStyleName("");
		if (stylePrefixes.containsKey(channel)) {
			label.addStyleName(stylePrefixes.get(channel));
		}
		if (fader != null) {
			fader.cancel();
		}
		SimplePanel holder = ft.holder;
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

		public FaderAnimation(SimplePanel holder) {
			this.holder = holder;
		}

		@Override
		protected void onComplete() {
			holder.setVisible(false);
		}

		double preFade = 0.7;

		@Override
		protected void onUpdate(double progress) {
			if (progress > preFade) {
				int opacityPercent = (int) (100 * (1 - (progress - preFade)
						/ (1 - preFade)));
				WidgetUtils.setOpacity(holder, opacityPercent);
			}
		}
	}

	public void removeWidget() {
		appTuple.label.removeFromParent();
		statusTuple.label.removeFromParent();
		centerTuple.label.removeFromParent();
		exceptionTuple.label.removeFromParent();
	}
}
