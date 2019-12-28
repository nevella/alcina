package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.gwt.client.lux.LuxStyle.LuxStyleStatus;

public class LuxStatusPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	public LuxStatusPanel() {
		initWidget(fp);
		LuxStyleStatus.LUX_STATUS_PANEL.add(this);
		LuxStyle.LUX.add(this);
		addAttachHandler(e -> {
			if (topicCallingRemote != null) {
				topicCallingRemote.delta(callingRemoteListener, e.isAttached());
			}
			if (topicMessage != null) {
				topicMessage.delta(messageListener, e.isAttached());
			}
		});
	}

	private TopicListener<Boolean> callingRemoteListener = (k, callingRemote) -> {
		fp.clear();
		if(callingRemote){
			fp.add(new Label("Calling remote"));
		}
	};

	private TopicListener<String> messageListener = (k, message) -> {
		fp.clear();
		if(Ax.notBlank(message)){
			fp.add(new Label(message));
		}
	};

	private TopicSupport<Boolean> topicCallingRemote;

	private TopicSupport<String> topicMessage;

	public void connectToTopics(TopicSupport<Boolean> topicCallingRemote,
			TopicSupport<String> topicMessage) {
		this.topicCallingRemote = topicCallingRemote;
		this.topicMessage = topicMessage;
	}
}