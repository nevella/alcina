package cc.alcina.framework.gwt.client.rpc;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstanceExpiredException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.common.client.util.TopicPublisher.Topic;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcTopics.ClientInstanceExpiredExceptionToken;

public class AlcinaRpcRequestBuilder extends RpcRequestBuilder {
	private static final String TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED = AlcinaRpcRequestBuilder.class
			.getName() + ".TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED";

	public static final String REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY = "X-ALCINA-CLIENT-INSTANCE-ID";

	public static final String REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY = "X-ALCINA-CLIENT-INSTANCE-AUTH";

	public static final String RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED = "X-ALCINA-CLIENT-INSTANCE-EXPIRED";

	public static AlcinaRpcRequestBuilderCreationOneOffReplayableListener
			addOneoffReplayableCreationListener() {
		AlcinaRpcRequestBuilderCreationOneOffReplayableListener listener = new AlcinaRpcRequestBuilderCreationOneOffReplayableListener();
		topicAlcinaRpcRequestBuilderCreated().add(listener);
		return listener;
	}

	private static Topic<AlcinaRpcRequestBuilder>
			topicAlcinaRpcRequestBuilderCreated() {
		return Topic.global(TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED);
	}

	protected boolean recordResult;

	protected Response response;

	private String payload;

	public AlcinaRpcRequestBuilder() {
		topicAlcinaRpcRequestBuilderCreated().publish(this);
	}

	public void addAlcinaHeaders(RequestBuilder rb) {
		addAlcinaHeaders(rb, true);
	}

	public void addAlcinaHeaders(RequestBuilder rb, boolean noCache) {
		// iOS 6
		if (noCache) {
			rb.setHeader("Cache-Control", "no-cache");
		}
		ClientInstance clientInstance = PermissionsManager.get()
				.getClientInstance();
		if (clientInstance != null) {
			rb.setHeader(REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY,
					String.valueOf(clientInstance.getId()));
			rb.setHeader(REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY,
					clientInstance.getAuth().toString());
		}
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
	}

	@Override
	protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
		callback = new WrappingCallback(callback);
		super.doSetCallback(rb, callback);
	}

	public static class AlcinaRpcRequestBuilderCreationOneOffReplayableListener
			implements TopicListener<AlcinaRpcRequestBuilder> {
		public AlcinaRpcRequestBuilder builder;

		@Override
		public void topicPublished(String key,
				AlcinaRpcRequestBuilder builder) {
			this.builder = builder;
			builder.setRecordResult(true);
			topicAlcinaRpcRequestBuilderCreated().remove(this);
		}
	}

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
			if (Ax.notBlank(response
					.getHeader(RESPONSE_HEADER_CLIENT_INSTANCE_EXPIRED))) {
				ClientInstanceExpiredExceptionToken token = new ClientInstanceExpiredExceptionToken();
				token.exception = new ClientInstanceExpiredException();
				AlcinaRpcTopics.topicClientInstanceExpiredException()
						.publish(token);
				if (token.handled) {
					return;
				}
			}
			originalCallback.onResponseReceived(request, response);
		}
	}
}
