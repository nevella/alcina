package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.EnvelopeId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageEnvelope;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessagePacket;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageToken;

public abstract class EnvelopeDispatcher {
	protected MessageTransportLayer transportLayer;

	/**
	 * <p>
	 * Marks a dispatcher subtype which tests dispatch exception handling
	 * <p>
	 * TODO - move to test tree
	 */
	public interface ForceExceptional {
	}

	public EnvelopeDispatcher(MessageTransportLayer transportLayer) {
		this.transportLayer = transportLayer;
	}

	protected abstract boolean isDispatchAvailable();

	protected abstract void dispatch(List<MessageToken> sendMessages,
			List<MessageToken> receivedMessages);

	/*
	 * the caller is synchronized on the unacknowledgedMessages list
	 */
	protected MessageEnvelope createEnvelope(List<MessageToken> sendMessages,
			List<MessageToken> receivedMessages) {
		MessageEnvelope envelope = new MessageEnvelope();
		envelope.envelopeId = new EnvelopeId(transportLayer.sendChannelId(),
				transportLayer.envelopeIdGenerator.incrementAndGetInt());
		envelope.dateSent = new Date();
		envelope.highestReceivedEnvelopeId = transportLayer
				.receiveChannel().highestReceivedEnvelopeId;
		sendMessages.stream().filter(MessageToken::shouldSend).forEach(uack -> {
			if (uack.transportHistory.firstReceiptAcknowledgedEnvelopeId == null) {
				uack.transportHistory.firstReceiptAcknowledgedEnvelopeId = envelope.envelopeId;
			}
			uack.onSending();
			envelope.packets.add(new MessagePacket(
					uack.transportHistory.messageId, uack.message));
		});
		/*
		 * send the transport histories of sent + received messages (for sender
		 * metrics)
		 */
		sendMessages.forEach(
				uack -> envelope.transportHistories.add(uack.transportHistory));
		receivedMessages.forEach(uack -> {
			uack.transportHistory
					.onBeforeSendReceivedMessageHistory(envelope.envelopeId);
			envelope.transportHistories.add(uack.transportHistory);
		});
		return envelope;
	}
}