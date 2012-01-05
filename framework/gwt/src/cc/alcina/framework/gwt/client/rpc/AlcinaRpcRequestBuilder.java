package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.gwt.client.ClientLayerLocator;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

public class AlcinaRpcRequestBuilder extends RpcRequestBuilder {
	public static final String CLIENT_INSTANCE_ID_KEY = "X-ALCINA-CLIENT-INSTANCE-ID";

	public static final String CLIENT_INSTANCE_AUTH_KEY = "X-ALCINA-CLIENT-INSTANCE-AUTH";

	private boolean recordResult;

	private Response response;

	private String payload;

	public void setResponsePayload(String payload) {
		this.payload = payload;
	}

	@Override
	protected RequestBuilder doCreate(String serviceEntryPoint) {
		if (payload != null) {
			return new SyncRequestBuilder(RequestBuilder.POST,
					serviceEntryPoint);
		}
		return super.doCreate(serviceEntryPoint);
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
				public String getText() {
					return payload;
				}

				@Override
				public String getStatusText() {
					return "OK";
				}

				@Override
				public int getStatusCode() {
					return 200;
				}

				@Override
				public String getHeadersAsString() {
					return "";
				}

				@Override
				public Header[] getHeaders() {
					return new Header[0];
				}

				@Override
				public String getHeader(String header) {
					return null;
				}
			};
			Request syncRequest = new SyncRequest();
			getCallback().onResponseReceived(syncRequest, response);
			return syncRequest;
		}
	}

	class SyncRequest extends Request {
		public SyncRequest() {
			super();
		}
	}

	@Override
	protected void doSetCallback(RequestBuilder rb, RequestCallback callback) {
		if (recordResult) {
			callback = new WrappingCallback(callback);
		}
		super.doSetCallback(rb, callback);
	}

	class WrappingCallback implements RequestCallback {
		private final RequestCallback originalCallback;

		public WrappingCallback(RequestCallback originalCallback) {
			this.originalCallback = originalCallback;
		}

		public void onError(Request request, Throwable exception) {
			originalCallback.onError(request, exception);
		}

		public void onResponseReceived(Request request, Response response) {
			AlcinaRpcRequestBuilder.this.response = response;
			originalCallback.onResponseReceived(request, response);
		}
	}

	@Override
	protected void doFinish(RequestBuilder rb) {
		super.doFinish(rb);
		addAlcinaHeaders(rb);
	}

	public void addAlcinaHeaders(RequestBuilder rb) {
		if (ClientLayerLocator.get().getClientInstance() != null
				&& PermissionsManager.get().isLoggedIn()) {
			rb.setHeader(
					CLIENT_INSTANCE_ID_KEY,
					String.valueOf(ClientLayerLocator.get().getClientInstance()
							.getId()));
			rb.setHeader(CLIENT_INSTANCE_AUTH_KEY, ClientLayerLocator.get()
					.getClientInstance().getAuth().toString());
		}
	}

	public void setRecordResult(boolean recordResult) {
		this.recordResult = recordResult;
	}

	public boolean isRecordResult() {
		return recordResult;
	}

	public String getRpcResult() {
		return response == null ? null : response.getText();
	}
}
