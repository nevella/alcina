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
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.TransportEvent.Type;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;

/**
 * <h3>The Message Transport Layer</h3>
 * <p>
 * This class provides the infrastructure to handle guaranteed, ordered
 * bidirectional message dispatch between the romcom server (jdk app) and romcom
 * client (the browser shim).
 * 
 * <p>
 * Messages are grouped into envelopes (the unit of send/receive). Both messages
 * and envelopes are numbered (with {@link MessageId}, {@link EnvelopeId} to
 * track receipt/dispatch.
 * 
 * <p>
 * The total communication flow is between two {@link MessageTransportLayer}
 * subtypes - <code>MessageTransportLayerServer</code> and
 * <code>MessageTransportLayerClient</code>. The two subtypes have different
 * {@link SendChannelId} values, which distinguish the two send/receive
 * channels.
 * <p>
 * Each {@link MessageTransportLayer} has two {@link Channel}s -
 * {@link SendChannel} + {@link ReceiveChannel}. They communicate by sending
 * {@link MessageEnvelope} instances over an underlying communication layer
 * (XmlHttp by default, but WebSocket would also work).
 * 
 * <h4>The protocol types - overview</h4>
 * <p>
 * A {@link MessageEnvelope} contains a list of {@link MessagePacket} elements
 * (the payload) and a list of {@link TransportHistory} elements (tracking
 * metadata).
 * 
 * <p>
 * A {@link MessagePacket} has a {@link MessageId} and a {@link Message}
 * 
 * <p>
 * A {@link TransportHistory} has a {@link MessageId} and various metadata
 * tracking the send/receive history of the message
 * 
 * A {@link MessageId} has a {@link SendChannelId} and a sequential, per-channel
 * <code>number</code>
 * 
 * @see SendChannel for detail on receipt verification - which handles
 *      communication issues causing RPC timeout or exception
 * 
 * 
 * 
 * 
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

		/*
		 * This will be cleared if a retry is required
		 */
		public Date sent;

		/*
		 * in the time context of the receiver
		 */
		public Date received;

		/*
		 * in the time context of the sender. Used by retry logic
		 */
		public List<Date> unacknowledgedSendDates = new ArrayList<>();

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

		public void markAsRetry() {
			unacknowledgedSendDates.add(sent);
			sent = null;
		}

		public boolean isPendingSend() {
			return shouldSend();
		}

		public boolean shouldSend() {
			return sent == null || (sendExceptionDate != null
					&& sent.before(sendExceptionDate));
		}

		public boolean wasSent() {
			return sent != null || unacknowledgedSendDates.size() > 0;
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

		@Override
		public String toString() {
			return Ax.format("%s :: %s", envelopeId, toMessageSummaryString());
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
			return transportHistory.shouldSend();
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

		transient int size = -1;

		public int getSize() {
			if (size == -1) {
				size = ReflectiveSerializer.serialize(message).length();
			}
			return size;
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

	public abstract class Channel {
		protected List<MessageToken> activeMessages = new ArrayList<>();

		protected Map<MessageId, MessageToken> messageIdActiveMessage = new LinkedHashMap<>();

		protected List<MessageToken> snapshotActiveMessages() {
			synchronized (activeMessages) {
				return new ArrayList<>(activeMessages);
			}
		}

		void bufferMessage(MessageToken message) {
			synchronized (activeMessages) {
				activeMessages.add(message);
				messageIdActiveMessage.put(message.transportHistory.messageId,
						message);
			}
		}

		void removeMessage(MessageToken message) {
			synchronized (activeMessages) {
				activeMessages.remove(message);
				messageIdActiveMessage
						.remove(message.transportHistory.messageId);
				logger.debug("Message acknowledged + removed :: {}",
						message.transportHistory.messageId);
			}
		}

		MessageToken getActiveMessage(MessageId messageId) {
			synchronized (activeMessages) {
				return messageIdActiveMessage.get(messageId);
			}
		}

		void removeAcknowledgedPublishedMessages() {
			synchronized (activeMessages) {
				List<MessageToken> toRemove = activeMessages.stream().filter(
						active -> active.acknowledged && (!publishesMessages()
								|| active.transportHistory.published != null))
						.collect(Collectors.toList());
				toRemove.forEach(this::removeMessage);
			}
		}

		/**
		 * true for receivechannel, false for sendchannel
		 */
		protected abstract boolean publishesMessages();

		@Override
		public String toString() {
			return Ax.format("active message count: %s", activeMessages.size());
		}
	}

	/**
	 * 
	 * <p>
	 * The SendChannel instance buffers and sends messages - grouped into
	 * envelopes - and ensures their eventual receipt.
	 * <p>
	 * The {@link EnvelopeDispatcher} is responsible for dispatching envelopes
	 * (invoking the RPC layer)
	 * 
	 * <h3>Message acknowledgment (ACK)</h3>
	 * <p>
	 * Messages are conditionally marked as requiring resend by the
	 * {@link ReceiptVerifier}if they're not acknowledged within a certain time,
	 * or transport fails due to an exception. The mechanics vary slightly for
	 * [server-&gt;client; client-&gt;server].
	 * 
	 * <h3>Issue recovery</h3>
	 * <p>
	 * Issue recovery is perfomed by the ReceiptVerifier - recovery is tied to
	 * acknowledgment - once a message is acknowledged, it's removed from the
	 * list of those potentially requiring issue recovery/retry.
	 * <h3>Testing: Issue recovery</h3>
	 * <p>
	 * Issue recovery is tested by forcing exceptional conditions at the RPC
	 * layer, both timeouts and transport exceptions. This can be done by
	 * instantiating an {@link EnvelopeDispatcher.ForceExceptional} subtype as
	 * the envelope dispatcher
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public abstract class SendChannel extends Channel {
		/*
		 * If no inflight or retry, send
		 */
		public void conditionallySend() {
			synchronized (activeMessages) {
				if (activeMessages.stream()
						.anyMatch(MessageToken::shouldSendMessageOrMetadata)) {
					if (envelopeDispatcher().isDispatchAvailable()) {
						unconditionallySend();
					}
				}
				receiptVerifier.verify();
			}
		}

		public void unconditionallySend() {
			synchronized (activeMessages) {
				envelopeDispatcher().dispatch(activeMessages,
						receiveChannel().snapshotActiveMessages());
			}
		}

		protected void send(Message message) {
			message.messageId = nextId();
			bufferMessage(new MessageToken(message, sendChannelId()));
			conditionallySend();
		}

		protected ReceiptVerifier receiptVerifier;

		public SendChannel() {
			initImplementations();
		}

		protected void initImplementations() {
			receiptVerifier = new ReceiptVerifier();
		}

		@Override
		protected boolean publishesMessages() {
			return false;
		}

		/**
		 * <p>
		 * THe ReceiptVerifier verifies receipt, by checking receipt after a
		 * certain elapsed time and if necessary signalling that unacknowledged
		 * messages should be resent
		 * <h4>Implementation logic</h4>
		 * <ul>
		 * <li>If there are no unacknowledged messages, exit
		 * <li>Compute the retry time for oldest packet, from [oldest send time;
		 * total in-flight size, retry count]
		 * <li>If retry time is exceeded, mark packets as requiring resend.
		 * Record send history; signal a conditional send; rerun the
		 * verification process (to ensure a timer)
		 * <li>Else, if there's an existing verification scheduled, exit
		 * <li>Else schedule a verification check for the retry time
		 * </ul>
		 * 
		 * <p>
		 * Both sender and receiver retain references to messages until receipt
		 * has been acknowledged by the other end of the connection. The
		 * mechanics for how acknowledgment is signalled differ:
		 * 
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
		 * <td>Event: send to other end (this will be triggered if there are
		 * outstanding messages *or* outstanding transporthistory.published
		 * metadata</td>
		 * <td>Send transportHistory of received packets. For any with
		 * firstReceiptAcknowledgedEnvelopeId==null, set
		 * firstReceiptAcknowledgedEnvelopeId</td>
		 * </tr>
		 * <tr>
		 * <td>(receiver channel) Event: messageReceived</td>
		 * <td/>
		 * <td/>
		 * <td/>
		 * </tr>
		 * <tr>
		 * <td>Action: process acknowledgement of any transporthistories owned
		 * by this sender with receivedDate set</td>
		 * <td/>
		 * <td/>
		 * <td/>
		 * </tr>
		 * </table>
		 */
		public class ReceiptVerifier {
			Timer scheduledVerification = null;

			boolean verifying;

			protected void verify() {
				if (verifying) {
					return;
				}
				synchronized (activeMessages) {
					try {
						verifying = true;
						verify0();
					} finally {
						verifying = false;
					}
				}
			}

			protected void verify0() {
				if (activeMessages.stream().allMatch(m -> m.acknowledged)) {
					return;
				}
				Date now = new Date();
				boolean retryMarked = false;
				int inFlightSize = activeMessages.stream()
						.filter(token -> token.transportHistory.wasSent())
						.collect(Collectors
								.summingInt(token -> token.getSize()));
				Date earliestRetry = null;
				for (MessageToken token : activeMessages) {
					/*
					 * An existing send (possibly retry) already exists for this
					 * token, ignore for retry computation
					 */
					if (token.transportHistory.isPendingSend()) {
						retryMarked = true;
						continue;
					}
					Date retryDate = computeRetryDate(token, inFlightSize);
					if (retryDate.compareTo(now) <= 0) {
						token.transportHistory.markAsRetry();
						logger.info("{} - retry scheduled",
								token.message.messageId);
						new MessageTransportLayerObservables.RetryObservable(
								token).publish();
						retryMarked = true;
					} else {
						if (earliestRetry == null
								|| retryDate.before(earliestRetry)) {
							earliestRetry = retryDate;
						}
					}
				}
				if (retryMarked) {
					conditionallySend();
				} else {
					Preconditions.checkState(earliestRetry != null);
					if (scheduledVerification == null) {
						scheduledVerification = Timer.Provider.get()
								.getTimer(this::onScheduledVerification);
						scheduledVerification.schedule(
								earliestRetry.getTime() - now.getTime());
					}
				}
			}

			void onScheduledVerification() {
				scheduledVerification = null;
				verify();
			}

			/*
			 * Non-computed, and doesn't allow for compression - just a
			 * heuristic
			 */
			protected int bandwidthBytesPerSecond() {
				return 500000;
			}

			/*
			 * min retry time : 1 second
			 * 
			 * max retry time (1st attempt): 5 seconds
			 * 
			 * backoff by retrytime multiplier (exponential)
			 */
			protected Date computeRetryDate(MessageToken messageToken,
					int inFlightSize) {
				long from = messageToken.transportHistory.sent.getTime();
				double transmissionTimeMs = inFlightSize
						/ bandwidthBytesPerSecond() * 1000.0;
				transmissionTimeMs = Math.max(1000.0, transmissionTimeMs);
				transmissionTimeMs = Math.min(5000.0, transmissionTimeMs);
				transmissionTimeMs = Math.pow(2,
						messageToken.transportHistory.unacknowledgedSendDates
								.size())
						* transmissionTimeMs;
				transmissionTimeMs = Math.min(60000.0, transmissionTimeMs);
				return new Date((long) transmissionTimeMs + from);
			}
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (activeMessages) {
				envelope.transportHistories.stream().filter(
						remoteHistory -> remoteHistory.messageId.sendChannelId == sendChannelId())
						.forEach(remoteHistory -> {
							MessageToken activeMessageToken = getActiveMessage(
									remoteHistory.messageId);
							if (activeMessageToken != null) {
								activeMessageToken
										.updateTransportHistoryFromRemote(
												remoteHistory);
							} else {
								int debug = 3;
							}
						});
				EnvelopeId highestReceivedEnvelopeId = envelope.highestReceivedEnvelopeId;
				if (highestReceivedEnvelopeId != null) {
					activeMessages.forEach(activeMessage -> activeMessage
							.onHighestReceivedEnvelopeId(
									highestReceivedEnvelopeId));
					removeAcknowledgedPublishedMessages();
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
			addMessagesToActive(envelope);
			publishSequentialMessages();
			sendChannel().sendAcknowledgments();
		}

		void publishSequentialMessages() {
			synchronized (activeMessages) {
				for (MessageToken activeMessageToken : activeMessages) {
					boolean publish = activeMessageToken.message.messageId == highestPublishedMessageId
							+ 1;
					publish |= handler(activeMessageToken.message)
							.isHandleOutOfBand();
					if (publish) {
						topicMessageReceived
								.publish(activeMessageToken.message);
						highestPublishedMessageId++;
						activeMessageToken.transportHistory.published = new Date();
					}
				}
			}
		}

		@Override
		protected boolean publishesMessages() {
			return true;
		}

		protected abstract Message.Handler handler(Message message);

		void addMessagesToActive(MessageEnvelope envelope) {
			/*
			 * this is the only place receivedMessageIds is used, so sync works
			 * for both
			 */
			synchronized (activeMessages) {
				envelope.packets.forEach(packet -> {
					if (!receivedMessageIds.add(packet.messageId)) {
						return;
					}
					MessageToken activeMessageToken = new MessageToken(
							packet.message, packet.messageId.sendChannelId);
					activeMessageToken.transportHistory.received = new Date();
					activeMessageToken.transportHistory.sent = envelope.dateSent;
					activeMessages.add(activeMessageToken);
				});
				Collections.sort(activeMessages);
			}
		}

		void updateHistoriesOnReceipt(MessageEnvelope envelope) {
			synchronized (activeMessages) {
				/*
				 * The highest envelopeId sent by *this* endpoint, received by
				 * the other
				 */
				EnvelopeId highestReceivedEnvelopeId = envelope.highestReceivedEnvelopeId;
				/*
				 * Any complete messages (that this channel has received) for
				 * which we know that receipt acknowledgment has reached the
				 * sender, remove
				 */
				if (highestReceivedEnvelopeId != null) {
					activeMessages.forEach(activeMessage -> activeMessage
							.onHighestReceivedEnvelopeId(
									highestReceivedEnvelopeId));
					removeAcknowledgedPublishedMessages();
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
