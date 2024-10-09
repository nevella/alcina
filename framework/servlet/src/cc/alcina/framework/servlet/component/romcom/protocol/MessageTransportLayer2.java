package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

/**
 * This (simple) class provides the infrastructure to handle lost requests, by
 * numbering and queueing message receipt/dispatch
 */
/*
 * A message transport (layer) has two channels - send + receive - each with an
 * enum id
 * 
 * A messageenvelope has messagepackets + messagedispatchhistories
 * 
 * A messagepacket has messageid - message
 * 
 * A transportHistory has messageid - timesent (if sender), timereceived (if
 * receiver)
 * 
 * A messageid has channelId + number (in channel)
 * 
 * An envelopeDispatcher is responsible for dispatching envelopes
 * 
 * A connectionVerifier is responsible for verifying the state of the connection
 * 
 * The way acknowledgment/receipt works is that a transporthistory has the first
 * envelopeId where receipt was sent - if a envelopeId higher than that is
 * received, receipt has been acknowledged and the packet can be released
 */
public abstract class MessageTransportLayer2 {
	@Reflected
	public enum SendChannelId {
		CLIENT_TO_SERVER, SERVER_TO_CLIENT
	}

	@Bean(PropertySource.FIELDS)
	public static class MessagePacket {
		public MessageId messageId;

		public Message message;

		public MessagePacket(MessageId messageId, Message message) {
			this.messageId = messageId;
			this.message = message;
		}

		public MessagePacket() {
		}

		public Message message() {
			return message;
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class MessageId implements Comparable<MessageId> {
		public SendChannelId sendChannelId;

		public int number;

		public MessageId() {
		}

		public MessageId(SendChannelId sendChannelId, int number) {
			this.sendChannelId = sendChannelId;
			this.number = number;
		}

		@Override
		public int compareTo(MessageId o) {
			Preconditions.checkState(sendChannelId == o.sendChannelId);
			return number - o.number;
		}

		@Override
		public String toString() {
			return Ax.format("#%s [%s]", number, sendChannelId);
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class EnvelopeId implements Comparable<EnvelopeId> {
		public static int nullAwareCompare(EnvelopeId o1, EnvelopeId o2) {
			return CommonUtils.compareWithNullMinusOne(o1, o2);
		}

		public SendChannelId sendChannelId;

		public int number;

		public EnvelopeId(SendChannelId sendChannelId, int number) {
			this.sendChannelId = sendChannelId;
			this.number = number;
		}

		public EnvelopeId() {
		}

		@Override
		public int compareTo(EnvelopeId o) {
			Preconditions.checkState(sendChannelId == o.sendChannelId);
			return number - o.number;
		}

		@Override
		public String toString() {
			return Ax.format("#%s [%s]", number, sendChannelId);
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class TransportHistory {
		public MessageId messageId;

		public EnvelopeId firstReceiptPacketId;

		public Date sent;

		/*
		 * in the time context of the receiver
		 */
		public Date received;

		/*
		 * in the time context of the sender
		 */
		public Date resendRequested;

		public Date published;

		public Date sendExceptionDate;

		void queuedForDispatch() {
			sent = new Date();
		}

		boolean wasAcknowledged(EnvelopeId highestReceivedEnvelopeId) {
			return firstReceiptPacketId != null && firstReceiptPacketId
					.compareTo(highestReceivedEnvelopeId) <= 0;
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class MessageEnvelope {
		public EnvelopeId envelopeId;

		public Date dateSent;

		public EnvelopeId highestReceivedEnvelopeId;

		public List<MessagePacket> packets = new ArrayList<>();

		public List<TransportHistory> transportHistories = new ArrayList<>();

		public String toMessageSummaryString() {
			return packets.stream().map(MessagePacket::message)
					.map(NestedName::get).collect(Collectors.joining(","));
		}

		public String toMessageDebugString() {
			return packets.stream().map(MessagePacket::message)
					.map(Message::toDebugString)
					.collect(Collectors.joining(","));
		}
	}

	public class UnacknowledgedMessage
			implements Comparable<UnacknowledgedMessage> {
		public TransportHistory transportHistory = new TransportHistory();

		public Message message;

		public UnacknowledgedMessage(Message message,
				SendChannelId sendChannelId) {
			if (message.messageId == 0) {
				message.messageId = messageIdGenerator.incrementAndGetInt();
			}
			this.message = message;
			this.transportHistory.messageId = new MessageId(sendChannelId,
					message.messageId);
		}

		@Override
		public int compareTo(UnacknowledgedMessage o) {
			return this.transportHistory.messageId
					.compareTo(o.transportHistory.messageId);
		}

		boolean shouldSend() {
			return transportHistory.sent == null
					|| (transportHistory.resendRequested != null
							&& transportHistory.sent
									.before(transportHistory.resendRequested))
					|| (transportHistory.sendExceptionDate != null
							&& transportHistory.sent.before(
									transportHistory.sendExceptionDate));
		}

		boolean wasAcknowledged(EnvelopeId highestReceivedEnvelopeId) {
			return transportHistory.wasAcknowledged(highestReceivedEnvelopeId);
		}

		public void onSendException(Throwable exception) {
			transportHistory.sendExceptionDate = new Date();
		}
	}

	public abstract class EnvelopeDispatcher {
		protected abstract boolean isDispatchAvailable();

		protected abstract void
				dispatch(List<UnacknowledgedMessage> unacknowledgedMessages);

		/*
		 * the caller is synchronized on the unacknowledgedMessages list
		 */
		protected MessageEnvelope createEnvelope(
				List<UnacknowledgedMessage> unacknowledgedMessages) {
			MessageEnvelope envelope = new MessageEnvelope();
			envelope.envelopeId = new EnvelopeId(sendChannelId(),
					envelopeIdGenerator.incrementAndGetInt());
			envelope.highestReceivedEnvelopeId = receiveChannel().highestReceivedEnvelopeId;
			unacknowledgedMessages.stream()
					.filter(UnacknowledgedMessage::shouldSend).forEach(uack -> {
						uack.transportHistory.sent = new Date();
						envelope.packets.add(new MessagePacket(
								uack.transportHistory.messageId, uack.message));
					});
			unacknowledgedMessages.forEach(uack -> {
				envelope.transportHistories.add(uack.transportHistory);
			});
			return envelope;
		}
	}

	public abstract class Channel {
		protected List<UnacknowledgedMessage> unacknowledgedMessages = new ArrayList<>();
	}

	public abstract class SendChannel extends Channel {
		protected void send(Message message) {
			message.messageId = nextId();
			bufferMessage(new UnacknowledgedMessage(message, sendChannelId()));
			conditionallySend();
		}

		/*
		 * If no inflight or retry, send
		 */
		public void conditionallySend() {
			synchronized (unacknowledgedMessages) {
				/*
				 * This path doesn't send if there's *only* receipt metadata to
				 * acknowledge - that's handled in the verifier side-channel
				 */
				if (unacknowledgedMessages.stream()
						.anyMatch(UnacknowledgedMessage::shouldSend)) {
					if (envelopeDispatcher().isDispatchAvailable()) {
						envelopeDispatcher().dispatch(unacknowledgedMessages);
					}
				}
			}
		}

		void bufferMessage(UnacknowledgedMessage message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.add(message);
			}
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (unacknowledgedMessages) {
				EnvelopeId highestReceivedEnvelopeId = envelope.highestReceivedEnvelopeId;
				if (highestReceivedEnvelopeId != null) {
					unacknowledgedMessages.removeIf(u -> {
						return u.wasAcknowledged(highestReceivedEnvelopeId);
					});
				}
			}
		}

		public void onReceiveSuccess() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'onReceiveSuccess'");
		}
	}

	public abstract class ReceiveChannel extends Channel {
		int highestPublishedMessageId = 0;

		EnvelopeId highestReceivedEnvelopeId;

		/*
		 * Send - update histories
		 * 
		 * emit message receipt
		 */
		public void onEnvelopeReceived(MessageEnvelope envelope) {
			if (EnvelopeId.nullAwareCompare(highestReceivedEnvelopeId,
					envelope.envelopeId) < 0) {
				highestReceivedEnvelopeId = envelope.envelopeId;
			}
			updateHistoriesOnReceipt(envelope);
			sendChannel().updateHistoriesOnReceipt(envelope);
			addMessagesToUnacknowledged(envelope);
			publishSequentialMessages();
		}

		void publishSequentialMessages() {
			synchronized (unacknowledgedMessages) {
				for (UnacknowledgedMessage unacknowledgedMessage : unacknowledgedMessages) {
					if (unacknowledgedMessage.message.messageId == highestPublishedMessageId
							+ 1) {
						topicMessageReceived
								.publish(unacknowledgedMessage.message);
						highestPublishedMessageId++;
						unacknowledgedMessage.transportHistory.published = new Date();
					}
				}
			}
		}

		void addMessagesToUnacknowledged(MessageEnvelope envelope) {
			synchronized (unacknowledgedMessages) {
				envelope.packets.forEach(packet -> {
					UnacknowledgedMessage unacknowledgedMessage = new UnacknowledgedMessage(
							packet.message, packet.messageId.sendChannelId);
					unacknowledgedMessage.transportHistory.received = new Date();
					unacknowledgedMessage.transportHistory.sent = envelope.dateSent;
					unacknowledgedMessages.add(unacknowledgedMessage);
				});
				Collections.sort(unacknowledgedMessages);
			}
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (unacknowledgedMessages) {
				/*
				 * The highest envelopeId sent by *this* endpoint, received by
				 * the other
				 */
				EnvelopeId highestReceivedEnvelopeId = envelope.highestReceivedEnvelopeId;
				/*
				 * Any unacknowledged messages (that this channel has received)
				 * for which we know that receipt acknowledgment has reached the
				 * sender, remove
				 */
				if (highestReceivedEnvelopeId != null) {
					unacknowledgedMessages.removeIf(u -> {
						return u.wasAcknowledged(highestReceivedEnvelopeId);
					});
				}
			}
		}
	}

	SequentialIdGenerator messageIdGenerator = new SequentialIdGenerator();

	SequentialIdGenerator envelopeIdGenerator = new SequentialIdGenerator();

	public Topic<RemoteComponentProtocol.Message> topicMessageReceived = Topic
			.create();

	/*
	 * TODO - remove
	 */
	public int nextId() {
		return messageIdGenerator.incrementAndGetInt();
	}

	public void sendMessage(RemoteComponentProtocol.Message message) {
		sendChannel().send(message);
	}

	protected abstract SendChannelId sendChannelId();

	protected abstract SendChannel sendChannel();

	protected abstract ReceiveChannel receiveChannel();

	protected abstract EnvelopeDispatcher envelopeDispatcher();
}
