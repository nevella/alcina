package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.EnvelopeDispatcher;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.EnvelopeId;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageEnvelope;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessagePacket;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

class EnvelopeDispatcherClient extends EnvelopeDispatcher {
	private MessageTransportLayerClient messageTransportLayerClient;

	EnvelopeDispatcherClient(
			MessageTransportLayerClient messageTransportLayerClient) {
		super(messageTransportLayerClient);
		this.messageTransportLayerClient = messageTransportLayerClient;
	}

	@Override
	protected boolean shouldSendReceiveChannelMetadata() {
		return true;
	}

	/*
	 * Because http/2 - there's no reason to not send messages as they come in
	 * (except possibly server load, but trying to gate that from the client is
	 * dubiousx)
	 */
	@Override
	@Feature.Ref(Feature_RemoteObjectComponent.Feature_ClientEventThrottling.class)
	protected boolean isDispatchAvailable() {
		boolean finished = (RemoteObjectModelComponentState.get().finished
				|| this.messageTransportLayerClient.willFinish);
		if (finished) {
			return false;
		}
		/*
		 * There's no need to send AwaitRemote messages if the tab is not
		 * visible, and it causes spam on console restart/remote server issues
		 */
		boolean shouldSend = Document.get().isVisibilityStateVisible()
				|| !this.messageTransportLayerClient.sendChannel()
						.activeMessagesAreAwaitRemoteOnly();
		/*
		 * TODO - possibly some better balance here between responsiveness and
		 * overwhelming (particularly with rapid keyboard events), calculation
		 * may also be better with latency factored in
		 */
		// shouldSend &= getInflightCount() <= 5;
		return shouldSend;
	}

	int getInflightCount() {
		return inflightEnvelope.size();
	}

	Map<EnvelopeId, EnvelopeTransportHistory> inflightEnvelope = new LinkedHashMap<>();

	class EnvelopeTransportHistory {
		MessageEnvelope envelope;

		EnvelopeTransportHistory(MessageEnvelope envelope) {
			this.envelope = envelope;
			id = envelope.envelopeId;
			sent = new Date();
		}

		Date sent;

		Date expectedReceipt;

		EnvelopeId id;

		String toDebugString() {
			FormatBuilder format = new FormatBuilder();
			format.line("envelope :: %s - sent :: %s", id, Ax.appMillis(sent));
			format.line("  transport history :: %s",
					envelope.toTransportDebugString());
			format.line("  messages :: %s", envelope.toMessageDebugString());
			return format.toString();
		}
	}

	public String toDebugString() {
		return inflightEnvelope.values().stream()
				.map(EnvelopeTransportHistory::toDebugString)
				.collect(Collectors.joining("\n"));
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
		request.session = this.messageTransportLayerClient.session;
		/*
		 * serialize, send as RPC
		 */
		String payload = ReflectiveSerializer.serializeForRpc(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		Date sendTime = new Date();
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request httpRequest, Throwable exception) {
				onReturned();
				messageTransportLayerClient.remoteExceptions.add(
						messageTransportLayerClient.new RemoteExceptionEvent(
								exception, sendTime));
				sendMessages.forEach(uak -> uak.onSendException(exception));
				messageTransportLayerClient.onRequestReturnedPostFinishCheck();
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
							new MessageTransportLayerClient.StatusCodeException(
									httpResponse));
					return;
				}
				messageTransportLayerClient.onReceiveSuccess();
				String text = httpResponse.getText();
				RemoteComponentResponse response = ReflectiveSerializer
						.deserializeRpc(text);
				getLogger().debug("envelope returned :: {} :: {}/{}",
						Ax.appMillis(), envelope.envelopeId,
						response.messageEnvelope.envelopeId);
				messageTransportLayerClient.willFinish |= response.messageEnvelope.packets
						.stream().map(MessagePacket::message).anyMatch(
								ProtocolMessageHandlerClient::isClientFinished);
				messageTransportLayerClient.onRequestReturnedPostFinishCheck();
				messageTransportLayerClient.onComponentResponse(response);
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