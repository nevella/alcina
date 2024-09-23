package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

/**
 * This (simple) class provides the infrastructure to handle lost requests, by
 * numbering and queueing message receipt/dispatch
 */
public abstract class MessageTransportLayer {
	SequentialIdGenerator idGenerator = new SequentialIdGenerator();

	public int nextId() {
		return (int) idGenerator.incrementAndGet();
	}

	public Topic<RemoteComponentProtocol.Message> topicSequentialMessageReceived = Topic
			.create();

	public void sendMessage(RemoteComponentProtocol.Message message) {
		sendChannel().send(message);
	}

	public static abstract class Channel {
	}

	protected abstract SendChannel sendChannel();

	public abstract class SendChannel extends Channel {
		protected List<Message> unacknowledgedMessages = new ArrayList<>();

		protected void send(Message message) {
			message.messageId = nextId();
			bufferMessage(message);
			conditionallySend();
		}

		/*
		 * If no inflight or retry, send
		 */
		protected abstract void conditionallySend();

		void bufferMessage(Message message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.add(message);
			}
		}
	}

	public static abstract class ReceiveChannel extends Channel {
	}
}
