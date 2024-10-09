package cc.alcina.framework.servlet.environment;

import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer2;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer.RequestToken;

class MessageTransportLayerServer extends MessageTransportLayer2 {
	class SendChannelImpl extends SendChannel {
	}

	class ReceiveChannelImpl extends ReceiveChannel {
	}

	class AggregateDispatcherImpl extends EnvelopeDispatcher {
		@Override
		protected boolean isDispatchAvailable() {
			return dispatchableToken != null;
		}

		@Override
		protected void
				dispatch(List<UnacknowledgedMessage> unacknowledgedMessages) {
			MessageEnvelope envelope = createEnvelope(unacknowledgedMessages);
			dispatchableToken.response.messageEnvelope = envelope;
			dispatchableToken.response.session = dispatchableToken.request.session;
			dispatchableToken.latch.countDown();
		}

		RequestToken dispatchableToken;

		/*
		 * never have more than one awaiting, dispatchable token
		 */
		void registerAvailableTokenForResponse(RequestToken token) {
			Preconditions.checkState(dispatchableToken == null);
			dispatchableToken = token;
		}
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

	SendChannelImpl sendChannel;

	ReceiveChannelImpl receiveChannel;

	AggregateDispatcherImpl aggregateDispatcher;

	MessageTransportLayerServer() {
		sendChannel = new SendChannelImpl();
		receiveChannel = new ReceiveChannelImpl();
		aggregateDispatcher = new AggregateDispatcherImpl();
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

	/*
	 * 
	 * Threading - this is not on the execution thread, so synchronized (it's
	 * just a quick routing of incoming data)
	 * 
	 */
	synchronized void onReceivedToken(RequestToken token) {
		/*
		 * distribute etc incoming messages
		 */
		receiveChannel().onEnvelopeReceived(token.request.messageEnvelope);
		/*
		 * if there's already a token, this will cause it to be sent [FIXME -
		 * signal 'finish buffering to-client messages']
		 */
		if (aggregateDispatcher.isDispatchAvailable()) {
			sendChannel.unconditionallySend();
		}
		/*
		 * register the associated HttpServletResponse for use as a protocol
		 * response
		 */
		aggregateDispatcher.registerAvailableTokenForResponse(token);
		sendChannel.conditionallySend();
	}
}
