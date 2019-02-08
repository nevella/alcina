package cc.alcina.extras.dev.console.remote.client.common.logic;

import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleRequest;
import cc.alcina.extras.dev.console.remote.protocol.RemoteConsoleResponse;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;

public class RemoteConsoleClientUtils {
    public static void runAsync(Class clazz, Runnable runnable) {
        GWT.runAsync(clazz, new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable reason) {
                throw new WrappedRuntimeException(reason);
            }

            @Override
            public void onSuccess() {
                runnable.run();
            }
        });
    }

    public static RunAsyncCallback runAsyncCallback(Runnable runnable) {
        return new RunAsyncCallback() {
            @Override
            public void onFailure(Throwable reason) {
                throw new WrappedRuntimeException(reason);
            }

            @Override
            public void onSuccess() {
                runnable.run();
            }
        };
    }

    public static void submitRequest(RemoteConsoleRequest request,
            Consumer<RemoteConsoleResponse> consoleResponseConsumer) {
        String payload = AlcinaBeanSerializer.serializeHolder(request);
        String href = "/remote-console.do";
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, href);
        RequestCallback callback = new RequestCallback() {
            @Override
            public void onError(Request request, Throwable exception) {
                throw new WrappedRuntimeException(exception);
            }

            @Override
            public void onResponseReceived(Request request, Response response) {
                String text = response.getText();
                RemoteConsoleResponse consoleResponse = text.isEmpty() ? null
                        : AlcinaBeanSerializer.deserializeHolder(text);
                if (consoleResponseConsumer != null) {
                    consoleResponseConsumer.accept(consoleResponse);
                }
            }
        };
        try {
            builder.sendRequest(payload, callback);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }
}
