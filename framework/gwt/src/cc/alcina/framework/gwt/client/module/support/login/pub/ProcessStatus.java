package cc.alcina.framework.gwt.client.module.support.login.pub;

import java.util.List;

import cc.alcina.framework.common.client.collections.IdentityArrayList;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed
// TODO - probably move to dirndl/model
public class ProcessStatus extends Model {
	private List<Message> messages = new IdentityArrayList<>();

	private TopicListener<Boolean> callingRemoteListener = callingRemote -> {
		clearMessages();
		if (callingRemote) {
			// addMessage("Calling remote", "loading");
		}
	};

	private TopicListener<String> messageListener = message -> {
		clearMessages();
		if (Ax.notBlank(message)) {
			addMessage(message, "error");
		}
	};

	private Topic<Boolean> topicCallingRemote;

	private Topic<String> topicMessage;

	public void addMessage(String text, String type) {
		IdentityArrayList<Message> newList = IdentityArrayList
				.add(getMessages(), new Message(text, type));
		setMessages(newList);
	}

	public void connectToTopics(Topic<Boolean> topicCallingRemote,
			Topic<String> topicMessage) {
		this.topicCallingRemote = topicCallingRemote;
		this.topicMessage = topicMessage;
	}

	@Directed
	public List<Message> getMessages() {
		return this.messages;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (topicCallingRemote != null) {
			topicCallingRemote.delta(callingRemoteListener, event.isBound());
		}
		if (topicMessage != null) {
			topicMessage.delta(messageListener, event.isBound());
		}
	}

	public void setMessages(List<Message> messages) {
		var old_messages = this.messages;
		this.messages = messages;
		propertyChangeSupport().firePropertyChange("messages", old_messages,
				messages);
	}

	private void clearMessages() {
		setMessages(new IdentityArrayList<>());
	}

	@Directed(
		bindings = { @Binding(from = "text", type = Type.INNER_TEXT),
				@Binding(from = "type", type = Type.PROPERTY, to = "class") })
	public static class Message extends Model {
		private final String text;

		private final String type;

		public Message(String text, String type) {
			this.text = text;
			this.type = type;
		}

		public String getText() {
			return this.text;
		}

		public String getType() {
			return this.type;
		}
	}
}