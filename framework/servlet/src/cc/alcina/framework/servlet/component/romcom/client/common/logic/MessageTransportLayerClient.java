package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Document;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.EnvelopeDispatcher;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

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
	}

	class ReceiveChannelImpl extends ReceiveChannel {
		@Override
		protected Message.Handler handler(Message message) {
			return Registry.impl(ProtocolMessageHandlerClient.class,
					message.getClass());
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

	class EnvelopeDispatcherImpl extends EnvelopeDispatcher {
		EnvelopeDispatcherImpl() {
			super(MessageTransportLayerClient.this);
		}

		@Override
		protected boolean shouldSendReceiveChannelMetadata() {
			return true;
		}

		/*
		 * Because http/2 - there's no reason to not send messages as they come
		 * in (except possibly server load, but trying to gate that from the
		 * client is dubiousx)
		 */
		@Override
		protected boolean isDispatchAvailable() {
			boolean finished = (RemoteObjectModelComponentState.get().finished
					|| willFinish);
			if (finished) {
				return false;
			}
			/*
			 * There's no need to send AwaitRemote messages if the tab is not
			 * visible, and it causes spam on console restart/remote server
			 * issues
			 */
			boolean shouldSend = Document.get().isVisibilityStateVisible()
					|| !sendChannel().activeMessagesAreAwaitRemoteOnly();
			return shouldSend;
		}

		int getInflightCount() {
			return inflightEnvelope.size();
		}

		Map<EnvelopeId, EnvelopeTransportHistory> inflightEnvelope = new LinkedHashMap<>();

		class EnvelopeTransportHistory {
			EnvelopeTransportHistory(MessageEnvelope envelope) {
				id = envelope.envelopeId;
				sent = new Date();
			}

			Date sent;

			Date expectedReceipt;

			EnvelopeId id;
		}

		@Override
		protected void dispatch(List<MessageToken> sendMessagesIn,
				List<MessageToken> receivedMessages) {
			/*
			 * make a copy of the parameter for post-rpc processing, avoid
			 * synchronization issues
			 */
			List<MessageToken> sendMessages = new ArrayList<>(sendMessagesIn);
			MessageEnvelope envelope = createEnvelope(sendMessages,
					receivedMessages);
			RemoteComponentRequest request = new RemoteComponentRequest();
			request.messageEnvelope = envelope;
			request.session = session;
			/*
			 * serialize, send as RPC
			 */
			String payload = ReflectiveSerializer.serializeForRpc(request);
			String path = Window.Location.getPath();
			RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,
					path);
			Date sendTime = new Date();
			RequestCallback callback = new RequestCallback() {
				@Override
				public void onError(Request httpRequest, Throwable exception) {
					onReturned();
					remoteExceptions
							.add(new RemoteExceptionEvent(exception, sendTime));
					sendMessages.forEach(uak -> uak.onSendException(exception));
					onRequestReturnedPostFinishCheck();
				}

				void onReturned() {
					inflightEnvelope.remove(envelope.envelopeId);
				}

				@Override
				public void onResponseReceived(Request httpRequest,
						Response httpResponse) {
					onReturned();
					if (httpResponse.getStatusCode() == 0
							|| httpResponse.getStatusCode() >= 400) {
						onError(httpRequest,
								new StatusCodeException(httpResponse));
						return;
					}
					onReceiveSuccess();
					String text = httpResponse.getText();
					RemoteComponentResponse response = ReflectiveSerializer
							.deserializeRpc(text);
					getLogger().debug("envelope returned :: {} :: {}/{}",
							Ax.appMillis(), envelope.envelopeId,
							response.messageEnvelope.envelopeId);
					willFinish |= response.messageEnvelope.packets.stream()
							.map(MessagePacket::message).anyMatch(
									ProtocolMessageHandlerClient::isClientFinished);
					onRequestReturnedPostFinishCheck();
					onComponentResponse(response);
				}
			};
			try {
				inflightEnvelope.put(envelope.envelopeId,
						new EnvelopeTransportHistory(envelope));
				builder.sendRequest(payload, callback);
				getLogger().debug("envelope sent :: {}", envelope.envelopeId);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	static class StatusCodeException extends Exception {
		Response httpResponse;

		StatusCodeException(Response httpResponse) {
			this.httpResponse = httpResponse;
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

	void debugProtocol() {
		ClientUtils.consoleInfo("A message");
	}

	native final void attachRpcDebugMethod() /*-{
		$wnd.__romcom_dp = function(){
		this.@cc.alcina.framework.servlet.component.romcom.client.common.logic.MessageTransportLayerClient::debugProtocol()();
		};
		}-*/;
}
