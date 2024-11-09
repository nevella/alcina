package cc.alcina.framework.servlet.environment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.EnvelopeDispatcher;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;

/**
 * <p>
 * The server end of the message transport layer
 * 
 * 
 * 
 * @see {@link MessageBatcher} - performance optimiser, which batches messages
 *      such as interleaved invoke/js and mutation
 */
class MessageTransportLayerServer extends MessageTransportLayer {
	class SendChannelImpl extends SendChannel {
		/**
		 * See {@link MessageTransportLayerServer#onReceivedToken} - we want to
		 * only have one higher-level operation accessing the envelopeDispatcher
		 */
		@Override
		public void conditionallySend() {
			synchronized (envelopeDispatcher()) {
				super.conditionallySend();
			}
		}

		@Override
		protected void send(Message message) {
			batcher.add(message);
		}

		/*
		 * re-implement to allow MessageBatcherServer to access this method
		 */
		protected void send(List<Message> messages) {
			super.send(messages);
		}
	}

	class ReceiveChannelImpl extends ReceiveChannel {
		public Date lastEnvelopeReceived = new Date(0);

		@Override
		public void onEnvelopeReceived(MessageEnvelope envelope) {
			lastEnvelopeReceived = new Date();
			super.onEnvelopeReceived(envelope);
		}

		@Override
		protected Message.Handler handler(Message message) {
			return Registry.impl(MessageHandlerServer.class,
					message.getClass());
		}
	}

	/**
	 * Thread access - check
	 */
	static class AggregateDispatcher extends EnvelopeDispatcher {
		static class ExceptionTest extends AggregateDispatcher {
			ExceptionTest(MessageTransportLayerServer transportLayer) {
				super(transportLayer);
			}

			@Override
			protected void dispatch(List<MessageToken> sendMessages,
					List<MessageToken> receivedMessages) {
				if (Math.random() < 0.1) {
					MessageEnvelope envelope = createEnvelope(sendMessages,
							receivedMessages);
					Ax.err("Simulate transport issue - dropping %s", envelope);
					RequestToken dispatchableToken = getPreferredDispatchableTokenAndRemoveFromAvailable();
					dispatchableToken.latch.countDown();
					return;
				} else {
					super.dispatch(sendMessages, receivedMessages);
				}
			}
		}

		List<DispatchableToken> dispatchableTokens = new ArrayList<>();

		class DispatchableToken {
			RequestToken token;

			DispatchableToken(RequestToken token) {
				this.token = token;
			}

			boolean isAwaiter() {
				return token.request.messageEnvelope.packets.stream()
						.anyMatch(pkt -> pkt.message instanceof AwaitRemote);
			}
		}

		RequestToken getPreferredDispatchableTokenAndRemoveFromAvailable() {
			// try non-awaiter
			DispatchableToken dispatchable = dispatchableTokens.stream()
					.filter(token -> !token.isAwaiter()).findFirst()
					.orElse(null);
			if (dispatchable == null) {
				// fall through to non-awaiter
				dispatchable = dispatchableTokens.stream()
						.filter(token -> token.isAwaiter()).findFirst().get();
			}
			dispatchableTokens.remove(dispatchable);
			return dispatchable.token;
		}

		/*
		 * Sync - all operations/calls should be synchronized on the
		 * AggregateDispatcher instance
		 */
		AggregateDispatcher(MessageTransportLayerServer transportLayer) {
			super(transportLayer);
		}

		@Override
		protected boolean isDispatchAvailable() {
			return dispatchableTokens.size() > 0;
		}

		@Override
		protected void dispatch(List<MessageToken> sendMessages,
				List<MessageToken> receivedMessages) {
			MessageEnvelope envelope = createEnvelope(sendMessages,
					receivedMessages);
			RequestToken dispatchableToken = getPreferredDispatchableTokenAndRemoveFromAvailable();
			dispatchableToken.response.messageEnvelope = envelope;
			dispatchableToken.response.session = dispatchableToken.request.session;
			dispatchableToken.latch.countDown();
		}

		/*
		 * any superfluous requests (not needed for return dispatch) will be
		 */
		void registerAvailableTokenForResponse(RequestToken token) {
			DispatchableToken dispatchable = new DispatchableToken(token);
			dispatchableTokens.add(dispatchable);
		}

		/*
		 * Allow at most one awaiter, one or zero non-awaiters
		 */
		boolean shouldFlush(boolean flushAllNonAwaiters) {
			return dispatchableTokens.stream()
					.filter(token -> !token.isAwaiter())
					.count() > (flushAllNonAwaiters ? 0 : 1)
					|| dispatchableTokens.stream()
							.filter(token -> token.isAwaiter()).count() > 1;
		}

		boolean hasDispatchers() {
			return dispatchableTokens.size() > 0;
		}
	}

	long start = System.currentTimeMillis();

	SendChannelImpl sendChannel;

	ReceiveChannelImpl receiveChannel;

	AggregateDispatcher aggregateDispatcher;

	MessageBatcherServer batcher;

	MessageTransportLayerServer() {
		sendChannel = new SendChannelImpl();
		receiveChannel = new ReceiveChannelImpl();
		aggregateDispatcher = new AggregateDispatcher(this);
		batcher = new MessageBatcherServer();
	}

	void onFinish() {
		synchronized (envelopeDispatcher()) {
			while (aggregateDispatcher.hasDispatchers()) {
				sendChannel.unconditionallySend();
			}
		}
	}

	@Override
	protected SendChannel sendChannel() {
		return sendChannel;
	}

	@Override
	protected SendChannelId sendChannelId() {
		return SendChannelId.SERVER_TO_CLIENT;
	}

	@Override
	protected EnvelopeDispatcher envelopeDispatcher() {
		return aggregateDispatcher;
	}

	@Override
	protected ReceiveChannel receiveChannel() {
		return receiveChannel;
	}

	long lifetimeMs() {
		return System.currentTimeMillis() - start;
	}

	/*
	 * If the envelope contains a startup packet, this will be false - so the
	 * new clientInstanceUid (session) in this envelope will clobber other
	 * sessions for the same logical Component, if any
	 */
	boolean isValidateClientInstanceUid(RemoteComponentRequest request) {
		return request.messageEnvelope.packets.stream().anyMatch(
				p -> handlerFor(p.message).isValidateClientInstanceUid());
	}

	MessageHandlerServer<?> handlerFor(Message message) {
		return Registry.impl(MessageHandlerServer.class, message.getClass());
	}

	/*
	 * 
	 * Threading - this is not on the execution thread, so synchronized (it's
	 * just a quick routing of incoming data). This synchronizes on the envelope
	 * dispatcher to ensure no races with
	 * 
	 */
	void onReceivedToken(RequestToken token) {
		synchronized (envelopeDispatcher()) {
			/*
			 * distribute etc incoming messages
			 */
			receiveChannel().onEnvelopeReceived(token.request.messageEnvelope);
			/*
			 * register the associated HttpServletResponse for use as a protocol
			 * response
			 */
			aggregateDispatcher.registerAvailableTokenForResponse(token);
			/*
			 * ensure the dispatcher doesn't have too many in-flight requests
			 * queued
			 */
			conditionallyFlushDispatcher(false);
			sendChannel.conditionallySend();
		}
	}

	void conditionallyFlushDispatcher(boolean flushAllNonAwaiters) {
		while (aggregateDispatcher.shouldFlush(flushAllNonAwaiters)) {
			sendChannel.unconditionallySend();
		}
	}

	Date getLastEnvelopeReceived() {
		return receiveChannel.lastEnvelopeReceived;
	}

	/*
	 * For easier debugging, wait until this point to flush any non-awaiter
	 * envelopes. That'll ensure that, in the normal case, the http roundtrip
	 * contains the incoming message and any response messages
	 * 
	 * Calls to this method will be immediately preceded by
	 * environment.access().flush();
	 */
	void flush() {
		batcher.flush();
	}

	/**
	 * <p>
	 * THe batcher groups as many messages as possible before sending, to
	 * improve performance.
	 * <p>
	 * Invariants are:
	 * <ul>
	 * <li>Outgoing messages must be sent once all incoming messages are
	 * processed
	 * <li>Callers of message.send guarantee that mutations are flushed (and the
	 * mutation message added, if any) before any dom-related message (i.e.
	 * invoke) are enqueued
	 * <li>Outgoing messages must be sent if a synchronous client response is
	 * required (i.e. invoke/sync was called)
	 * <li>Effectively this means an interleaved sequence of [invoke, mutation]
	 * messages are sent
	 * </ul>
	 */
	class MessageBatcherServer {
		synchronized void flush() {
			List<Message> toSend = messages;
			messages = new ArrayList<>();
			sendChannel.send(toSend);
			conditionallyFlushDispatcher(true);
		}

		List<Message> messages = new ArrayList<>();

		synchronized void add(Message message) {
			messages.add(message);
		}
	}
}
