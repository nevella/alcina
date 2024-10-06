package cc.alcina.framework.servlet.environment;

import java.util.List;

import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer2;

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
}
