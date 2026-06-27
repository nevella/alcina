package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.http.client.Response;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.EnvelopeDispatcherClient.EnvelopeTransportHistory;
import cc.alcina.framework.servlet.component.romcom.protocol.EnvelopeDispatcher;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.Mutations;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ServerDebugProtocolRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.StringProtocol;

/**
 * <p>
 * FIXME - DOC
 * 
 * <p>
 * Transport is complicated by acknowledgment. Round #3 - 20241120 - we need to
 * send an extra awaitresponses (essentially) as the transport/metadata carrier
 * - it will be released by the server, ensuring a complete client/server
 * metadata cycle
 */
public class MessageTransportLayerClient extends MessageTransportLayer {
	RemoteComponentProtocol.Session session;

	/*
	 * a lookahead in response processing to skip metadata handshake if the
	 * response indiciates this session is finished
	 */
	boolean willFinish;

	/*
	 * set during prepending
	 */
	boolean inPrepareMessagePhase;

	class SendChannelImpl extends SendChannel {
		/*
		 * If there are no inflight envelopes -- OR there's a single
		 * unacknowledged AwaitRemote in flight -- enqueue an await message
		 */
		void conditionallyEnqueueAwaitMessage() {
			if (envelopeDispatcher.getInflightCount() == 0) {
				send(new AwaitRemote());
				send(new AwaitRemote());
			}
		}

		boolean logNextDispatch;

		@Override
		protected boolean shouldSendMessagesOrMetadata() {
			if (inPrepareMessagePhase) {
				return false;
			}
			synchronized (MessageTransportLayerClient.this) {
				if (activeMessages.stream()
						.anyMatch(MessageToken::shouldSendMetadata)) {
					return true;
				}
				List<MessageToken> shouldSend = activeMessages.stream()
						.filter(MessageToken::shouldSend).toList();
				if (shouldSend.size() > 0) {
					if (envelopeDispatcher.inflightEnvelope.size() > 3) {
						int debug = 3;
					}
					if (activeMessages.stream().map(MessageToken::getMessage)
							.allMatch(
									MessageTransportLayerClient::isContinuousEventOrWindowUpdate)) {
						List<EnvelopeTransportHistory> inflight = envelopeDispatcher
								.getInflightContinuousEventEnvelopes();
						if (inflight.size() > 2) {
							logNextDispatch = true;
							return false;
						}
					}
					if (logNextDispatch) {
						logNextDispatch = false;
					}
					return true;
				} else {
					return false;
				}
			}
		}
	}

	static boolean isContinuousEventOrWindowUpdate(Message message) {
		return message instanceof Message.WindowStateUpdate
				|| Message.DomEventMessage.isContinuousEventMessage(message);
	}

	class ReceiveChannelImpl extends ReceiveChannel {
		@Override
		protected Message.Handler handler(Message message) {
			return Registry.impl(ProtocolMessageHandlerClient.class,
					message.getClass());
		}

		Message lastMutations = null;

		@Override
		protected void publishSequentialMessages() {
			for (int idx = activeMessages.size() - 1; idx >= 0; idx--) {
				if (activeMessages.get(idx).message instanceof Mutations) {
					lastMutations = activeMessages.get(idx).message;
					break;
				}
			}
			super.publishSequentialMessages();
			lastMutations = null;
		}
	}

	/*
	 * used in backoff logic
	 */
	List<RemoteExceptionEvent> remoteExceptions = new ArrayList<>();

	class RemoteExceptionEvent {
		Date sendDate;

		Date receiveDate;

		Throwable throwable;

		RemoteExceptionEvent(Throwable throwable, Date sendDate) {
			this.throwable = throwable;
			this.sendDate = sendDate;
			this.receiveDate = new Date();
		}
	}

	/*
	 * must be called late (since client.finish must be checked before
	 * enqueueing awaits)
	 */
	void onRequestReturnedPostFinishCheck() {
		sendChannel.conditionallyEnqueueAwaitMessage();
		sendChannel().conditionallySend();
	}

	void onComponentResponse(RemoteComponentResponse response) {
		receiveChannel().onEnvelopeReceived(response.messageEnvelope);
	}

	static class StatusCodeException extends Exception {
		Response httpResponse;

		StatusCodeException(Response httpResponse) {
			this.httpResponse = httpResponse;
		}
	}

	SendChannelImpl sendChannel;

	ReceiveChannelImpl receiveChannel;

	EnvelopeDispatcherClient envelopeDispatcher;

	StringProtocol.Cache stringProtocolCache;

	String remotePath;

	public StringProtocol.Cache getStringProtocolCache() {
		return stringProtocolCache;
	}

	MessageTransportLayerClient(StringProtocol.Cache stringProtocolCache,
			String remotePath) {
		this.stringProtocolCache = stringProtocolCache;
		this.remotePath = remotePath;
		sendChannel = new SendChannelImpl();
		receiveChannel = new ReceiveChannelImpl();
		envelopeDispatcher = new EnvelopeDispatcherClient(this);
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

	void debugProtocol() {
		FormatBuilder format = new FormatBuilder();
		format.line("Protocol state: client");
		format.dashedLine();
		format.line("Active messages:");
		format.append(sendChannel().toActiveStateString());
		format.line("Inflight envelopes:");
		format.append(envelopeDispatcher.toDebugString());
		String clientState = format.toString();
		ClientUtils.consoleInfo(clientState);
		ServerDebugProtocolRequest debugProtocolRequest = new ServerDebugProtocolRequest();
		debugProtocolRequest.clientState = clientState;
		sendMessage(debugProtocolRequest);
	}

	native final void attachRpcDebugMethod() /*-{
		var self=this;
		$wnd.__romcom_dp = function(){
		self.@cc.alcina.framework.servlet.component.romcom.client.common.logic.MessageTransportLayerClient::debugProtocol()();
		};
		}-*/;

	static MessageTransportLayerClient cast() {
		return (MessageTransportLayerClient) get();
	}
}
