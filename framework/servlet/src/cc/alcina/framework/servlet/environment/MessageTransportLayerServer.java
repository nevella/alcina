package cc.alcina.framework.servlet.environment;

import java.util.List;

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
			return false;
		}

		@Override
		protected void
				dispatch(List<UnacknowledgedMessage> unacknowledgedMessages) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'dispatch'");
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
 * @formatter:off
 * 
 * distribute etc incoming messages
 * 
 * await empty to-process queue (unless a from-server synchronous message needs sending)
 * 
 * send outgoing messages
 * 
 * * @formatter:on
 */
	void onReceivedToken(RequestToken token) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'onReceivedToken'");
	}
}
