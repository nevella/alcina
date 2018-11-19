package cc.alcina.framework.gwt.client.rpc;

import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public class AlcinaRpcRequestBuilder extends RpcRequestBuilder {
    public static final String TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED = AlcinaTopics.class
            .getName() + ".TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED";

    public static void alcinaRpcRequestBuilderCreated(
            AlcinaRpcRequestBuilder createdBuilder) {
        GlobalTopicPublisher.get().publishTopic(
                TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED, createdBuilder);
    }

    public static void alcinaRpcRequestBuilderCreatedListenerDelta(
            TopicListener<AlcinaRpcRequestBuilder> listener, boolean add) {
        GlobalTopicPublisher.get().listenerDelta(
                TOPIC_ALCINA_RPC_REQUEST_BUILDER_CREATED, listener, add);
    }

    public static class AlcinaRpcRequestBuilderCreationOneOffReplayableListener
            implements TopicListener<AlcinaRpcRequestBuilder> {
        public AlcinaRpcRequestBuilder builder;

        @Override
        public void topicPublished(String key,
                AlcinaRpcRequestBuilder builder) {
            this.builder = builder;
            builder.setRecordResult(true);
            alcinaRpcRequestBuilderCreatedListenerDelta(this, false);
        }
    }

    public static final String CLIENT_INSTANCE_ID_KEY = "X-ALCINA-CLIENT-INSTANCE-ID";

    public static final String CLIENT_INSTANCE_AUTH_KEY = "X-ALCINA-CLIENT-INSTANCE-AUTH";

    protected boolean recordResult;

    protected Response response;

    private String payload;

    public AlcinaRpcRequestBuilder() {
        alcinaRpcRequestBuilderCreated(this);
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
            rb.setHeader(CLIENT_INSTANCE_ID_KEY,
                    String.valueOf(clientInstance.getId()));
            rb.setHeader(CLIENT_INSTANCE_AUTH_KEY,
                    clientInstance.getAuth().toString());
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

    public static AlcinaRpcRequestBuilderCreationOneOffReplayableListener addOneoffReplayableCreationListener() {
        AlcinaRpcRequestBuilderCreationOneOffReplayableListener listener = new AlcinaRpcRequestBuilderCreationOneOffReplayableListener();
        alcinaRpcRequestBuilderCreatedListenerDelta(listener, true);
        return listener;
    }
}
