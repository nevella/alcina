package cc.alcina.framework.gwt.client.widget.complex;

import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

public class StatusDisplayer implements StateChangeListener {
	FaderAnimation faderAnimation = null;

	private static final int FADER_DURATION = 5000;

	private Label statusLabel;

	StateChangeListener messageListener = new StateChangeListener() {
		public void stateChanged(Object source, String newState) {
			showMessage(newState, true);
		}
	};

	private MouseOverHandler overHandler=new MouseOverHandler() {
		@Override
		public void onMouseOver(MouseOverEvent event) {
			if (faderAnimation != null) {
				faderAnimation.cancel();
			}
		}
	};;;

	public StatusDisplayer() {
	}

	public void attach() {
		this.statusLabel = new Label();
		statusLabel.addMouseOverHandler(overHandler);
		statusLabel.setStyleName("alcina-Status");
		statusLabel.setVisible(false);
		CallManager.get().addStateChangeListener(this);
		MessageManager.get().addStateChangeListener(messageListener);
		RootPanel.get().add(statusLabel);
	}

	public void detach() {
		CallManager.get().removeStateChangeListener(this);
		MessageManager.get().removeStateChangeListener(messageListener);
	}

	public void stateChanged(Object source, String newState) {
		showMessage(newState, false);
	}

	private void showMessage(String message, boolean withFade) {
		statusLabel.removeStyleName("icy");
		if (message != null && message.startsWith(MessageManager.ICY_MESSAGE)) {
			message = message.substring(MessageManager.ICY_MESSAGE.length());
			statusLabel.addStyleName("icy");
		}
		if (faderAnimation != null) {
			faderAnimation.cancel();
		}
		WidgetUtils.setOpacity(statusLabel, 0);
		statusLabel.setVisible(message != null);
		statusLabel.setText(message);
		int w = statusLabel.getOffsetWidth();
		int bw = Window.getClientWidth();
		// statusLabel.getElement().getStyle().setProperty("right","");
		// statusLabel.getElement().getStyle().setPropertyPx("top", 70);
		// statusLabel.getElement().getStyle().setPropertyPx("left", (bw - w) /
		// 2);
		WidgetUtils.setOpacity(statusLabel, 100);
		if (withFade) {
			faderAnimation = new FaderAnimation();
			faderAnimation.run(FADER_DURATION);
		}
	}

	private class FaderAnimation extends Animation {
		@Override
		protected void onComplete() {
			statusLabel.setVisible(false);
		}

		@Override
		protected void onUpdate(double progress) {
			WidgetUtils.setOpacity(statusLabel, (int) (100 * (1 - progress)));
		}
	}

	public void removeWidget() {
		statusLabel.removeFromParent();
	}
}
