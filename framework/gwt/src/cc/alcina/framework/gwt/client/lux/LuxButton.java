package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.gwt.client.widget.APanel;

public class LuxButton extends Composite {
	private APanel panel = new APanel<>();

	private InlineLabel label;
	private SimplePanel asyncIndicator;

	private boolean performingAsync = false;

	public LuxButton() {
		initWidget(panel);
		LuxButtonStyle.LUX_BUTTON.addTo(this);
	}

	public LuxButton withText(String text) {
		ensureLabel().setText(text);
		return this;
	}

	private InlineLabel ensureLabel() {
		if (label == null) {
			label = new InlineLabel();
			LuxButtonStyle.LABEL.addTo(label);
			panel.add(label);
		}
		return label;
	}
	private SimplePanel ensureAsyncIndicator() {
		if (asyncIndicator == null) {
			asyncIndicator = new SimplePanel();
			LuxButtonStyle.ASYNC_INDICATOR.addTo(asyncIndicator);
			panel.add(asyncIndicator);
			SimplePanel child = new SimplePanel();
			asyncIndicator.add(child);
		}
		return asyncIndicator;
	}

	public void setPerformingAsync(boolean performingAsync) {
		this.performingAsync = performingAsync;
		ensureLabel().setVisible(!this.performingAsync);
		ensureAsyncIndicator().setVisible(this.performingAsync);
	}

	public LuxButton withHandler(ClickHandler clickHandler) {
		panel.addClickHandler(event -> {
			if (!performingAsync) {
				clickHandler.onClick(event);
			}
		});
		return this;
	}

	public LuxButton withAsyncTopic(TopicSupport<Boolean> topicAsync) {
		TopicListener<Boolean> asyncListener = (k, v) -> setPerformingAsync(v);
		addAttachHandler(e -> topicAsync.delta(asyncListener, e.isAttached()));
		return this;
	}
}
