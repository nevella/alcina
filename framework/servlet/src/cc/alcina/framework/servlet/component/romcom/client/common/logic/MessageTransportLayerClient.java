package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer2;

public class MessageTransportLayerClient extends MessageTransportLayer2 {
	class SendChannelImpl extends SendChannel {
	}

	class ReceiveChannelImpl extends ReceiveChannel {
	}

	ReceiptCallback messageCallback;

	ReceiptCallback verifierCallback;

	List<ReceiptCallback> failureHistory = new ArrayList<>();

	class EnvelopeDispatcherImpl extends EnvelopeDispatcher {
		/*
		 * if *inflight* - no. Note that the in-flight XMLHttp request can be
		 * cancelled by the verifier (or removed on success/failure/xmlhttp
		 * timeout)
		 */
		@Override
		protected boolean isDispatchAvailable() {
			return messageCallback == null;
		}

		@Override
		protected void
				dispatch(List<UnacknowledgedMessage> unacknowledgedMessages) {
			MessageEnvelope envelope = createEnvelope(unacknowledgedMessages);
			/*
			 * serialize, send as RPC
			 */
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'dispatch'");
		}
	}

	class ReceiptCallback implements AsyncCallback<MessageEnvelope> {
		Throwable caught;

		MessageEnvelope sent;

		Date created = new Date();

		ReceiptCallback(MessageEnvelope sent) {
			this.sent = sent;
		}

		@Override
		public void onFailure(Throwable caught) {
			onReceived();
			this.caught = caught;
			failureHistory.add(this);
			sendChannel();// conditionalsend
		}

		void onReceived() {
			if (verifierCallback == this) {
				verifierCallback = null;
			}
			if (messageCallback == this) {
				messageCallback = null;
			}
		}

		@Override
		public void onSuccess(MessageEnvelope result) {
			receiveChannel().onEnvelopeReceived(result);
		}
	}

	SendChannelImpl sendChannel;

	ReceiveChannelImpl receiveChannel;

	EnvelopeDispatcherImpl envelopeDispatcher;

	MessageTransportLayerClient() {
		sendChannel = new SendChannelImpl();
		receiveChannel = new ReceiveChannelImpl();
		envelopeDispatcher = new EnvelopeDispatcherImpl();
	}

	@Override
	protected SendChannelId sendChannelId() {
		return SendChannelId.CLIENT_TO_SERVER;
	}

	@Override
	protected SendChannel sendChannel() {
		return sendChannel;
	}

	@Override
	protected ReceiveChannel receiveChannel() {
		return receiveChannel;
	}

	@Override
	protected EnvelopeDispatcher envelopeDispatcher() {
		return envelopeDispatcher;
	}
}
