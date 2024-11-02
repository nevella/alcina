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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.SequentialIdGenerator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.TransportEvent.Type;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

/**
 * <p>
 * This (simple) class provides the infrastructure to handle lost requests, by
 * numbering and queueing message receipt/dispatch
 * 
 * <h3>Message acknowledgment (ACK)</h3>
 * <p>
 * Both sender and receiver retain references to messages until receipt has been
 * acknowledged by the other end of the connection. The mechanics for how
 * acknowledgment is signalled differ:
 * 
 * <?xml version="1.0" encoding="UTF-8"?>
 * <table>
 * <tr>
 * <td>Sender</td>
 * <td>Sender (impl)</td>
 * <td>Receiver</td>
 * <td>Receiver (impl)</td>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>(previous send)</td>
 * <td>Sends envelope with id R:M0</td>
 * </tr>
 * <tr>
 * <td>State: New message token, unsent</td>
 * <td>MessageToken object created, added to unacknowledgedTokens</td>
 * <td/>
 * <td/>
 * </tr>
 * <tr>
 * <td>Event: message sent</td>
 * <td/>
 * <td>message sent in envelope with id S:N0"</td>
 * <td/>
 * </tr>
 * <tr>
 * <td>Note that this envelope also contains the id of the last received
 * envelope (R:M0)" ",",</td>
 * <td/>
 * <td/>
 * <td/>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>Event: envelope received</td>
 * <td>TransportHistory.Received set</td>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>Event: envelope published</td>
 * <td>TransportHistory.Published set</td>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>State: message published</td>
 * <td/>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>... (other messages)</td>
 * <td/>
 * </tr>
 * <tr>
 * <td/>
 * <td/>
 * <td>Event: send to other end (this will be triggered if there are outstanding
 * messages *or* outstanding transporthistory.published metadata</td>
 * <td>Send transportHistory of received packets. For any with
 * publishedFirstSet==null, set publishedFirstSent to the outgoing envelope
 * id</td>
 * </tr>
 * <tr>
 * <td>(receiver channel) Event: messageReceived</td>
 * <td/>
 * <td/>
 * <td/>
 * </tr>
 * <tr>
 * <td>Action: process acknowledgement of any transporthistories owned by this
 * sender with publicationDate set</td>
 * <td/>
 * <td/>
 * <td/>
 * </tr>
 * </table>
 * 
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
/*
 * TODO - fix
 * 
 * there's confusion between *message* and *metadata* transport here - 'ack'
 * (s->r) should just be 'received' metadata (go back to 'update from received
 * metadata'); ack (r->s) should be 'firstReceivedEnvId'
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
		 * by this TransportHistory [sic] No- other way - env id is about r2s,
		 * not s2r
		 * 
		 * The reason for this layered approach is that the recipient doesn't
		 * need to send complex acknowledgments of receipt - only the envelope
		 * ID (although the sender does need to possibly double-send ) (tru -
		 * but only r2s)
		 * 
		 * A TODO is to not immediately re-send large messages - note this means
		 * not resending *anything*, since the contract is
		 * "resend everything unacknowledged". (nope, just meta)
		 * 
		 * So...in brief (and this para should go into a more structured
		 * location) -
		 * 
		 * send logic is
		 * "don't send until all outstanding sends are done, or no large outstanding sends"
		 * - say 300ms (small), 2s (large)
		 * 
		 * receipt logic is "always acknowledge transports"
		 * 
		 * resend logic is 'schedule a resend - cancel if no unacked'
		 * 
		 * Note - the sender *could* check published (to determine ACK) - but
		 * since the receiver *has* to check envelopeid, for symmetry I
		 * preferred to use the same mechanism for both endws of ACK
		 */
		/*
		 * This is the first envelope sent from the message receiver which
		 * contains 'received' metadata
		 */
		public EnvelopeId firstReceiptAcknowledgedEnvelopeId;

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

		boolean wasAcknowledged(SendChannelId sendChannelId,
				EnvelopeId highestReceivedEnvelopeId) {
			boolean isSenderEndpoint = sendChannelId == messageId.sendChannelId;
			if (isSenderEndpoint) {
				return received != null;
			} else {
				return firstReceiptAcknowledgedEnvelopeId != null
						&& firstReceiptAcknowledgedEnvelopeId
								.compareTo(highestReceivedEnvelopeId) <= 0;
			}
		}

		public void onBeforeSendReceivedMessageHistory(EnvelopeId envelopeId) {
			if (received != null
					&& firstReceiptAcknowledgedEnvelopeId == null) {
				firstReceiptAcknowledgedEnvelopeId = envelopeId;
			}
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

	/**
	 * This class tracks the lifecycle of a message, in particular it handles
	 * the 'retain until acknowledged' described in the outer class javadoc,
	 * which is required for resend
	 */
	public class MessageToken implements Comparable<MessageToken> {
		public TransportHistory transportHistory = new TransportHistory();

		public Message message;

		/*
		 * this isn't a property of the transport history, since its computation
		 * differs if the containing channel is sender or receiver
		 */
		boolean acknowledged;

		public MessageToken(Message message, SendChannelId sendChannelId) {
			if (message.messageId == 0) {
				message.messageId = messageIdGenerator.incrementAndGetInt();
			}
			this.message = message;
			this.transportHistory.messageId = new MessageId(sendChannelId,
					message.messageId);
		}

		@Override
		public int compareTo(MessageToken o) {
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

		boolean shouldSendMetadata() {
			return transportHistory.received != null
					&& transportHistory.firstReceiptAcknowledgedEnvelopeId == null;
		}

		boolean shouldSendMessageOrMetadata() {
			return shouldSend() || shouldSendMetadata();
		}

		void updateTransportHistoryFromRemote(TransportHistory remoteHistory) {
			if (remoteHistory.received != null
					&& transportHistory.received == null) {
				transportHistory.received = remoteHistory.received;
				new MessageTransportLayerObservables.ReceivedObservable(this)
						.publish();
			}
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

		public void onHighestReceivedEnvelopeId(
				EnvelopeId highestReceivedEnvelopeId) {
			if (transportHistory.wasAcknowledged(sendChannelId(),
					highestReceivedEnvelopeId)) {
				acknowledged = true;
			}
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

		protected abstract void dispatch(List<MessageToken> sendMessages,
				List<MessageToken> receivedMessages);

		/*
		 * the caller is synchronized on the unacknowledgedMessages list
		 */
		protected MessageEnvelope createEnvelope(
				List<MessageToken> sendMessages,
				List<MessageToken> receivedMessages) {
			MessageEnvelope envelope = new MessageEnvelope();
			envelope.envelopeId = new EnvelopeId(sendChannelId(),
					envelopeIdGenerator.incrementAndGetInt());
			envelope.dateSent = new Date();
			envelope.highestReceivedEnvelopeId = receiveChannel().highestReceivedEnvelopeId;
			sendMessages.stream().filter(MessageToken::shouldSend)
					.forEach(uack -> {
						if (uack.transportHistory.firstReceiptAcknowledgedEnvelopeId == null) {
							uack.transportHistory.firstReceiptAcknowledgedEnvelopeId = envelope.envelopeId;
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
			receivedMessages.forEach(uack -> {
				uack.transportHistory.onBeforeSendReceivedMessageHistory(
						envelope.envelopeId);
				envelope.transportHistories.add(uack.transportHistory);
			});
			return envelope;
		}
	}

	public abstract class Channel {
		protected List<MessageToken> unacknowledgedMessages = new ArrayList<>();

		protected Map<MessageId, MessageToken> messageIdUnacknowledgedMessage = new LinkedHashMap<>();

		protected List<MessageToken> snapshotUnacknowledgedMessages() {
			synchronized (unacknowledgedMessages) {
				return new ArrayList<>(unacknowledgedMessages);
			}
		}

		void bufferMessage(MessageToken message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.add(message);
				messageIdUnacknowledgedMessage
						.put(message.transportHistory.messageId, message);
			}
		}

		void removeMessage(MessageToken message) {
			synchronized (unacknowledgedMessages) {
				unacknowledgedMessages.remove(message);
				messageIdUnacknowledgedMessage
						.remove(message.transportHistory.messageId);
				logger.info("Message acknowledged + removed :: {}",
						message.transportHistory.messageId);
			}
		}

		MessageToken getUnacknowledgedMessage(MessageId messageId) {
			synchronized (unacknowledgedMessages) {
				return messageIdUnacknowledgedMessage.get(messageId);
			}
		}

		void removeAcknowledgedMessages() {
			synchronized (unacknowledgedMessages) {
				List<MessageToken> toRemove = unacknowledgedMessages.stream()
						.filter(uack -> uack.acknowledged)
						.collect(Collectors.toList());
				toRemove.forEach(this::removeMessage);
			}
		}

		@Override
		public String toString() {
			return Ax.format("uack message count: %s",
					unacknowledgedMessages.size());
		}
	}

	public abstract class SendChannel extends Channel {
		/*
		 * If no inflight or retry, send
		 */
		public void conditionallySend() {
			synchronized (unacknowledgedMessages) {
				if (unacknowledgedMessages.stream()
						.anyMatch(MessageToken::shouldSendMessageOrMetadata)) {
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
			bufferMessage(new MessageToken(message, sendChannelId()));
			conditionallySend();
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (unacknowledgedMessages) {
				envelope.transportHistories.stream().filter(
						remoteHistory -> remoteHistory.messageId.sendChannelId == sendChannelId())
						.forEach(remoteHistory -> {
							MessageToken unacknowledgedMessage = getUnacknowledgedMessage(
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
					unacknowledgedMessages
							.forEach(uack -> uack.onHighestReceivedEnvelopeId(
									highestReceivedEnvelopeId));
					removeAcknowledgedMessages();
				}
			}
		}

		void sendAcknowledgments() {
			conditionallySend();
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
			sendChannel().sendAcknowledgments();
		}

		void publishSequentialMessages() {
			synchronized (unacknowledgedMessages) {
				for (MessageToken unacknowledgedMessage : unacknowledgedMessages) {
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
			/*
			 * this is the only place receivedMessageIds is used, so sync works
			 * for both
			 */
			synchronized (unacknowledgedMessages) {
				envelope.packets.forEach(packet -> {
					if (!receivedMessageIds.add(packet.messageId)) {
						return;
					}
					MessageToken unacknowledgedMessage = new MessageToken(
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
					unacknowledgedMessages
							.forEach(uack -> uack.onHighestReceivedEnvelopeId(
									highestReceivedEnvelopeId));
					removeAcknowledgedMessages();
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

	protected Logger logger = LoggerFactory
			.getLogger(MessageTransportLayer.class);

	@Override
	public String toString() {
		return FormatBuilder.keyValues("sendChannelId", sendChannelId(),
				"sendChannel", sendChannel(), "receiveChannel",
				receiveChannel());
	}
}
