package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

public class MessageTransportLayerClient extends MessageTransportLayer {
	RemoteComponentProtocol.Session session;

	class SendChannelImpl extends SendChannel {
		/*
		 * If there are no inflight envelopes, enqueue an await message
		 */
		void conditionallyEnqueueAwaitMessage() {
			if (envelopeDispatcher.getInflightCount() == 0) {
				send(new AwaitRemote());
			}
		}
	}

	class ReceiveChannelImpl extends ReceiveChannel {
		protected Message.Handler handler(Message message) {
			return Registry.impl(ProtocolMessageHandlerClient.class,
					message.getClass());
		}
	}

	ReceiptCallback messageCallback;

	ReceiptCallback verifierCallback;

	List<ReceiptCallback> failureHistory = new ArrayList<>();

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

	void onRequestReturned() {
		sendChannel.conditionallyEnqueueAwaitMessage();
		sendChannel().conditionallySend();
	}

	void onComponentResponse(RemoteComponentResponse response) {
		receiveChannel().onEnvelopeReceived(response.messageEnvelope);
	}

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
		protected void
				dispatch(List<UnacknowledgedMessage> unacknowledgedMessagesIn) {
			/*
			 * make a copy of the parameter for post-rpc processing, avoid
			 * synchronization issues
			 */
			List<UnacknowledgedMessage> unacknowledgedMessages = new ArrayList<>(
					unacknowledgedMessagesIn);
			MessageEnvelope envelope = createEnvelope(unacknowledgedMessages);
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
					unacknowledgedMessages
							.forEach(uak -> uak.onSendException(exception));
				}

				void onReturned() {
					inflightEnvelope.remove(envelope.envelopeId);
					onRequestReturned();
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
					onComponentResponse(response);
				}
			};
			try {
				inflightEnvelope.put(envelope.envelopeId,
						new EnvelopeTransportHistory(envelope));
				builder.sendRequest(payload, callback);
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

	// FIXME - this isn't used
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
