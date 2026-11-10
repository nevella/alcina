package cc.alcina.framework.gwt.client.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import cc.alcina.framework.gwt.client.util.AsyncCallback;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;

public class AlcinaRpcInvoker implements ReflectiveRpcRemoteServiceAsync {
	protected String getRpcEndpoint() {
		return "/alcina-rpc.do";
	}

	static Logger logger = LoggerFactory.getLogger(AlcinaRpcInvoker.class);

	public void callRpc(String encodedRpcPayload,
			AsyncCallback<String> rpcCallback) {
		final String href = getRpcEndpoint();
		logger.info("dispatch/html: {}", href);
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, href);
		new AlcinaRpcRequestBuilder().addAlcinaHeaders(builder, false);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request request, Throwable exception) {
				logger.info("dispatch/html: WARN - {} - {}", exception, href);
				rpcCallback.onFailure(exception);
			}

			@Override
			public void onResponseReceived(Request request, Response response) {
				logger.info("dispatch/html: OK - {}", href);
				rpcCallback.onSuccess(response.getText());
			}
		};
		try {
			Request response = builder.sendRequest(encodedRpcPayload, callback);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
