package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.TransportEvent.Type;
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
 * 
 * NOTE - packet resend is currently *NOT* implemented
 */
public abstract class MessageTransportLayer {
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
		public boolean equals(Object obj) {
			if (obj instanceof MessageId) {
				MessageId o = (MessageId) obj;
				return sendChannelId == o.sendChannelId && number == o.number;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return sendChannelId.hashCode() ^ number;
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
		public boolean equals(Object obj) {
			if (obj instanceof EnvelopeId) {
				EnvelopeId o = (EnvelopeId) obj;
				return sendChannelId == o.sendChannelId && number == o.number;
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return sendChannelId.hashCode() ^ number;
		}

		@Override
		public String toString() {
			return Ax.format("#%s [%s]", number, sendChannelId);
		}
	}

	@Bean(PropertySource.FIELDS)
	public static class TransportHistory {
		public MessageId messageId;

		/*
		 * When a message is first sent, this is set (to the envelope id ID0 of
		 * the containing envelope, which will be the lowest envelope id for
		 * which this message was sent). Once the other end responds with
		 * "I have seen an envelope with id ID1", where ID1>=ID0, the sending
		 * logic guarantees that the receipient has received the message wrapped
		 * by this TransportHistory
		 * 
		 * The reason for this layered approach is that the recipient doesn't
		 * need to send complex acknowledgments of receipt - only the envelope
		 * ID (although the sender does need to possibly double-send )
		 * 
		 * A TODO is to not immediately re-send large messages - note this means
		 * not resending *anything*, since the contract is
		 * "resend everything unacknowledged"
		 */
		public EnvelopeId firstSentEnvelopeId;

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
			return firstSentEnvelopeId != null && firstSentEnvelopeId
					.compareTo(highestReceivedEnvelopeId) <= 0;
		}

		void updateFromOtherEndpoint(TransportHistory history) {
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
					.map(Object::toString).collect(Collectors.joining(","));
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

		public void onSendException(Throwable exception) {
			transportHistory.sendExceptionDate = new Date();
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

		void updateTransportHistoryFromRemote(TransportHistory remoteHistory) {
			if (remoteHistory.published != null
					&& transportHistory.published == null) {
				transportHistory.published = remoteHistory.published;
				new MessageTransportLayerObservables.PublishedObservable(this)
						.publish();
			}
		}

		public void onSending() {
			boolean resending = transportHistory.sent != null;
			transportHistory.sent = new Date();
			new MessageTransportLayerObservables.SentObservable(this, resending)
					.publish();
		}
	}

	public static class TransportEvent {
		public enum Type {
			success, transport_failure;
		}

		Date date;

		public Type type;

		public TransportEvent(Type type) {
			this.date = new Date();
			this.type = type;
		}
	}

	class TransportEvents {
		List<TransportEvent> events = new ArrayList<>();

		synchronized void onTransportSuccess() {
			events.add(new TransportEvent(Type.success));
		}

		synchronized void onTransportFailure() {
			events.add(new TransportEvent(Type.transport_failure));
		}
	}

	TransportEvents transportEvents = new TransportEvents();

	public void onReceiveSuccess() {
		transportEvents.onTransportSuccess();
	}

	public abstract class EnvelopeDispatcher {
		protected abstract boolean isDispatchAvailable();

		protected abstract void dispatch(
				List<UnacknowledgedMessage> sendMessages,
				List<UnacknowledgedMessage> receivedMessages);

		/*
		 * the caller is synchronized on the unacknowledgedMessages list
		 */
		protected MessageEnvelope createEnvelope(
				List<UnacknowledgedMessage> sendMessages,
				List<UnacknowledgedMessage> receivedMessages) {
			MessageEnvelope envelope = new MessageEnvelope();
			envelope.envelopeId = new EnvelopeId(sendChannelId(),
					envelopeIdGenerator.incrementAndGetInt());
			envelope.highestReceivedEnvelopeId = receiveChannel().highestReceivedEnvelopeId;
			sendMessages.stream().filter(UnacknowledgedMessage::shouldSend)
					.forEach(uack -> {
						if (uack.transportHistory.firstSentEnvelopeId == null) {
							uack.transportHistory.firstSentEnvelopeId = envelope.envelopeId;
						}
						uack.onSending();
						envelope.packets.add(new MessagePacket(
								uack.transportHistory.messageId, uack.message));
					});
			/*
			 * send the transport histories of sent + received messages (for
			 * sender metrics)
			 */
			sendMessages.forEach(uack -> envelope.transportHistories
					.add(uack.transportHistory));
			receivedMessages.forEach(uack -> envelope.transportHistories
					.add(uack.transportHistory));
			return envelope;
		}
	}

	public abstract class Channel {
		protected List<UnacknowledgedMessage> unacknowledgedMessages = new ArrayList<>();

		protected Map<MessageId, UnacknowledgedMessage> messageIdUnacknowledgedMessage = new LinkedHashMap<>();

		protected List<UnacknowledgedMessage> snapshotUnacknowledgedMessages() {
			synchronized (unacknowledgedMessages) {
				return new ArrayList<>(unacknowledgedMessages);
			}
		}

		void bufferMessage(UnacknowledgedMessage message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.add(message);
				messageIdUnacknowledgedMessage
						.put(message.transportHistory.messageId, message);
			}
		}

		void removeMessage(UnacknowledgedMessage message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.remove(message);
				messageIdUnacknowledgedMessage
						.remove(message.transportHistory.messageId);
			}
		}

		UnacknowledgedMessage getUnacknowledgedMessage(MessageId messageId) {
			synchronized (unacknowledgedMessages) {
				return messageIdUnacknowledgedMessage.get(messageId);
			}
		}
	}

	public abstract class SendChannel extends Channel {
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
						unconditionallySend();
					}
				}
			}
		}

		public void unconditionallySend() {
			synchronized (unacknowledgedMessages) {
				envelopeDispatcher().dispatch(unacknowledgedMessages,
						receiveChannel().snapshotUnacknowledgedMessages());
			}
		}

		protected void send(Message message) {
			message.messageId = nextId();
			bufferMessage(new UnacknowledgedMessage(message, sendChannelId()));
			conditionallySend();
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (unacknowledgedMessages) {
				envelope.transportHistories.stream().filter(
						remoteHistory -> remoteHistory.messageId.sendChannelId == sendChannelId())
						.forEach(remoteHistory -> {
							UnacknowledgedMessage unacknowledgedMessage = getUnacknowledgedMessage(
									remoteHistory.messageId);
							if (unacknowledgedMessage != null) {
								unacknowledgedMessage
										.updateTransportHistoryFromRemote(
												remoteHistory);
							} else {
								int debug = 3;
							}
						});
				EnvelopeId highestReceivedEnvelopeId = envelope.highestReceivedEnvelopeId;
				if (highestReceivedEnvelopeId != null) {
					unacknowledgedMessages.removeIf(u -> {
						return u.wasAcknowledged(highestReceivedEnvelopeId);
					});
				}
			}
		}
	}

	public abstract class ReceiveChannel extends Channel {
		int highestPublishedMessageId = 0;

		EnvelopeId highestReceivedEnvelopeId;

		protected Set<MessageId> receivedMessageIds = new LinkedHashSet<>();

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
					boolean publish = unacknowledgedMessage.message.messageId == highestPublishedMessageId
							+ 1;
					publish |= handler(unacknowledgedMessage.message)
							.isHandleOutOfBand();
					if (publish) {
						topicMessageReceived
								.publish(unacknowledgedMessage.message);
						highestPublishedMessageId++;
						unacknowledgedMessage.transportHistory.published = new Date();
					}
				}
			}
		}

		protected abstract Message.Handler handler(Message message);

		void addMessagesToUnacknowledged(MessageEnvelope envelope) {
			// this is the only place receivedMessageIds is used, so sync
			// works
			// for both
			synchronized (unacknowledgedMessages) {
				envelope.packets.forEach(packet -> {
					if (!receivedMessageIds.add(packet.messageId)) {
						return;
					}
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
					List<UnacknowledgedMessage> toRemove = unacknowledgedMessages
							.stream()
							.filter(u -> u
									.wasAcknowledged(highestReceivedEnvelopeId))
							.collect(Collectors.toList());
					toRemove.forEach(this::removeMessage);
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
