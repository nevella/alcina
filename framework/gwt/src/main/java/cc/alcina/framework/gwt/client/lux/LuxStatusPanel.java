package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleStatus;

public class LuxStatusPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	private TopicListener<Boolean> callingRemoteListener = callingRemote -> {
		fp.clear();
		if (callingRemote) {
			LuxWidgets.withText("Calling remote")
					.withStyle(LuxStyleStatus.LOADING).addTo(fp);
		}
	};

	private TopicListener<String> messageListener = message -> {
		fp.clear();
		if (Ax.notBlank(message)) {
			LuxWidgets.withText(message).withStyle(LuxStyleStatus.ERROR)
					.addTo(fp);
		}
	};

	private Topic<Boolean> topicCallingRemote;

	private Topic<String> topicMessage;

	public LuxStatusPanel() {
		initWidget(fp);
		LuxStyleStatus.LUX_STATUS_PANEL.addTo(this);
		LuxStyle.LUX.addTo(this);
		addAttachHandler(e -> {
			if (topicCallingRemote != null) {
				topicCallingRemote.delta(callingRemoteListener, e.isAttached());
			}
			if (topicMessage != null) {
				topicMessage.delta(messageListener, e.isAttached());
			}
		});
	}

	public void connectToTopics(Topic<Boolean> topicCallingRemote,
			Topic<String> topicMessage) {
		this.topicCallingRemote = topicCallingRemote;
		this.topicMessage = topicMessage;
	}
}