package cc.alcina.extras.dev.component.remote.client.common.logic;

import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentRequest;
import cc.alcina.extras.dev.component.remote.protocol.RemoteComponentResponse;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

public class RemoteComponentClientUtils {
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

	public static void submitRequest(RemoteComponentRequest request,
			Consumer<RemoteComponentResponse> consoleResponseConsumer) {
		String payload = ReflectiveSerializer.serialize(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request request, Throwable exception) {
				throw new WrappedRuntimeException(exception);
			}

			@Override
			public void onResponseReceived(Request request, Response response) {
				String text = response.getText();
				RemoteComponentResponse consoleResponse = text.isEmpty() ? null
						: ReflectiveSerializer.deserialize(text);
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
