package cc.alcina.framework.gwt.client.rpc;

import java.util.List;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstanceExpiredException;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcTopics.ClientInstanceExpiredExceptionToken;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage.OutOfBandMessageHandler;

public class AlcinaRpcRequestBuilder extends RpcRequestBuilder {
	public static final String REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY = "X-ALCINA-CLIENT-INSTANCE-ID";

	public static final String REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY = "X-ALCINA-CLIENT-INSTANCE-AUTH";

	public static final String RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED = "X-ALCINA-CLIENT-INSTANCE-EXPIRED";

	public static final String RESPONSE_HEADER_OUT_OF_BAND_MESSAGES = "X-ALCINA-OUT-OF-BAND-MESSAGES";

	public static final Topic<AlcinaRpcRequestBuilder> topicAlcinaRpcRequestBuilderCreated = Topic
			.create();

	public static final Topic<RequestBuilder> topicPostFlush = Topic.create();

	public static AlcinaRpcRequestBuilderCreationOneOffReplayableListener
			addOneoffReplayableCreationListener() {
		AlcinaRpcRequestBuilderCreationOneOffReplayableListener listener = new AlcinaRpcRequestBuilderCreationOneOffReplayableListener();
		topicAlcinaRpcRequestBuilderCreated.add(listener);
		return listener;
	}

	protected boolean recordResult;

	protected Response response;

	private String payload;

	public AlcinaRpcRequestBuilder() {
		topicAlcinaRpcRequestBuilderCreated.publish(this);
	}

	public void addAlcinaHeaders(RequestBuilder rb) {
		addAlcinaHeaders(rb, true);
	}

	public void addAlcinaHeaders(RequestBuilder requestBuilder,
			boolean noCache) {
		// iOS 6
		if (noCache) {
			requestBuilder.setHeader("Cache-Control", "no-cache");
		}
		ClientInstance clientInstance = Permissions.get().getClientInstance();
		if (clientInstance != null) {
			requestBuilder.setHeader(REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY,
					String.valueOf(clientInstance.getId()));
			requestBuilder.setHeader(REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY,
					clientInstance.getAuth().toString());
		}
		Registry.impl(ApplicationHeaders.class).addHeaders(requestBuilder);
	}

	@Override
	protected RequestBuilder doCreate(String serviceEntryPoint) {
		if (payload != null) {
			return new SyncRequestBuilder(RequestBuilder.POST,
					serviceEntryPoint);
		}
		return super.doCreate(serviceEntryPoint);
	}

	@Override
	protected void doFinish(RequestBuilder rb) {
		super.doFinish(rb);
		addAlcinaHeaders(rb);
		topicPostFlush.publish(rb);
	}

	@Override
	protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
		callback = new WrappingCallback(callback);
		super.doSetCallback(rb, callback);
	}

	public String getRpcResult() {
		return response == null ? null : response.getText();
	}

	public boolean isRecordResult() {
		return recordResult;
	}

	public void setRecordResult(boolean recordResult) {
		this.recordResult = recordResult;
	}

	public AlcinaRpcRequestBuilder setResponsePayload(String payload) {
		this.payload = payload;
		return this;
	}

	public static class AlcinaRpcRequestBuilderCreationOneOffReplayableListener
			implements TopicListener<AlcinaRpcRequestBuilder> {
		public AlcinaRpcRequestBuilder builder;

		@Override
		public void topicPublished(AlcinaRpcRequestBuilder builder) {
			this.builder = builder;
			builder.setRecordResult(true);
			topicAlcinaRpcRequestBuilderCreated.remove(this);
		}
	}

	@Reflected
	@Registration.Singleton
	public static class ApplicationHeaders {
		public void addHeaders(RequestBuilder requestBuilder) {
		}
	}

	/*
	 * Used for client-side replay of an async request
	 */
	class SyncRequest extends Request {
		public SyncRequest() {
			super();
		}
	}

	class SyncRequestBuilder extends RequestBuilder {
		public SyncRequestBuilder(Method httpMethod, String url) {
			super(httpMethod, url);
		}

		public SyncRequestBuilder(String httpMethod, String url) {
			super(httpMethod, url);
		}

		@Override
		public Request send() throws RequestException {
			Response response = new Response() {
				@Override
				public String getHeader(String header) {
					return null;
				}

				@Override
				public Header[] getHeaders() {
					return new Header[0];
				}

				@Override
				public String getHeadersAsString() {
					return "";
				}

				@Override
				public int getStatusCode() {
					return 200;
				}

				@Override
				public String getStatusText() {
					return "OK";
				}

				@Override
				public String getText() {
					return payload;
				}
			};
			Request syncRequest = new SyncRequest();
			getCallback().onResponseReceived(syncRequest, response);
			return syncRequest;
		}
	}

	class WrappingCallback implements RequestCallback {
		private final RequestCallback originalCallback;

		public WrappingCallback(RequestCallback originalCallback) {
			this.originalCallback = originalCallback;
		}

		@Override
		public void onError(Request request, Throwable exception) {
			originalCallback.onError(request, exception);
		}

		@Override
		public void onResponseReceived(Request request, Response response) {
			if (recordResult) {
				AlcinaRpcRequestBuilder.this.response = response;
			}
			String expiredHeader = response
					.getHeader(RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED);
			if (Ax.notBlank(expiredHeader)) {
				ClientInstanceExpiredExceptionToken token = new ClientInstanceExpiredExceptionToken();
				token.exception = new ClientInstanceExpiredException();
				token.expiredInstance = expiredHeader;
				AlcinaRpcTopics.topicClientInstanceExpiredException
						.publish(token);
				if (token.handled) {
					return;
				}
			}
			String outOfBandMessagesSerialized = response
					.getHeader(RESPONSE_HEADER_OUT_OF_BAND_MESSAGES);
			if (Ax.notBlank(outOfBandMessagesSerialized)) {
				List<OutOfBandMessage> messages = TransformManager.Serializer
						.get().deserialize(outOfBandMessagesSerialized);
				for (OutOfBandMessage outOfBandMessage : messages) {
					Registry.impl(OutOfBandMessageHandler.class,
							outOfBandMessage.getClass())
							.handle(outOfBandMessage);
				}
			}
			originalCallback.onResponseReceived(request, response);
		}
	}
}
