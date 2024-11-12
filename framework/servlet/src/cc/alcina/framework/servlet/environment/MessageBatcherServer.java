package cc.alcina.framework.servlet.environment;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

/**
 * <p>
 * THe batcher groups as many messages as possible before sending, to improve
 * performance.
 * <p>
 * Invariants are:
 * <ul>
 * <li>Outgoing messages must be sent once all incoming messages are processed
 * <li>Callers of message.send guarantee that mutations are flushed (and the
 * mutation message added, if any) before any dom-related message (i.e. invoke)
 * are enqueued
 * <li>Outgoing messages must be sent if a synchronous client response is
 * required (i.e. invoke/sync was called)
 * <li>Effectively this means an interleaved sequence of [invoke, mutation]
 * messages are sent
 * </ul>
 */
class MessageBatcherServer {
	MessageTransportLayerServer transportLayer;

	MessageBatcherServer(MessageTransportLayerServer transportLayer) {
		this.transportLayer = transportLayer;
	}

	synchronized void flush() {
		List<Message> toSend = messages;
		messages = new ArrayList<>();
		transportLayer.sendChannel.send(toSend);
		transportLayer.conditionallyFlushDispatcher(true);
	}

	List<Message> messages = new ArrayList<>();

	synchronized void add(Message message) {
		messages.add(message);
	}
}
