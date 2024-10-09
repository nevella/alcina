package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer2;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

public class MessageTransportLayerClient extends MessageTransportLayer2 {
	RemoteComponentProtocol.Session session;

	class SendChannelImpl extends SendChannel {
	}

	class ReceiveChannelImpl extends ReceiveChannel {
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
					remoteExceptions
							.add(new RemoteExceptionEvent(exception, sendTime));
					unacknowledgedMessages
							.forEach(uak -> uak.onSendException(exception));
					signalCalled(httpRequest);
				}

				void signalCalled(Request httpRequest) {
					onRequestReturned();
				}

				@Override
				public void onResponseReceived(Request httpRequest,
						Response httpResponse) {
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
					// if (response != null) {
					// // reset delay (successful response)
					// awaitDelay = 0;
					// Message message = response.protocolMessage;
					// Class<? extends Message> requestMessageClass =
					// request.protocolMessage
					// .getClass();
					// // Ax.out("[server->client response] #%s :: [client
					// message
					// // :: %s] ==> %s",
					// // response.requestId,
					// // NestedName.get(requestMessageClass),
					// // message == null ? "[null response]"
					// // : NestedName.get(message));
					// if (message != null) {
					// ProtocolMessageHandlerClient handler = Registry.impl(
					// ProtocolMessageHandlerClient.class,
					// message.getClass());
					// try {
					// handler.handle(response, message);
					// } catch (Throwable e) {
					// Ax.out("Exception handling message %s\n"
					// + "================\nSerialized form:\n%s",
					// message, text);
					// e.printStackTrace();
					// /*
					// * FIXME - devex - 0 - once syncmutations.3 is
					// * stable, this should not occur (ha!)
					// *
					// * Serious, the romcom client is a bounded piece of
					// * code that just propagates server changes to the
					// * client dom, so all exceptions *should* be
					// * server-only (unless client dom is mashed by an
					// * extension)
					// */
					// Window.alert(
					// CommonUtils.toSimpleExceptionMessage(e));
					// }
					// } else {
					// // Ax.out("Received no-message response for %s",
					// // NestedName.get(messageClass));
					// }
					// signalCalled(httpRequest);
					// } else {
					// }
				}
			};
			try {
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
