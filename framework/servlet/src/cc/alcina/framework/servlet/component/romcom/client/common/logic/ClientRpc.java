package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedNameProvider;
import cc.alcina.framework.servlet.component.romcom.protocol.ProtocolMessage;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;
import cc.alcina.framework.servlet.component.romcom.protocol.ProtocolMessage.AwaitRemote;

/*
 * Nice thing about statics is they *ensure* statelessness
 */
public class ClientRpc {
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

	public static void submitRequest(RemoteComponentRequest request) {
		String payload = ReflectiveSerializer.serialize(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request request, Throwable exception) {
				throw new WrappedRuntimeException(exception);
			}

			@Override
			public void onResponseReceived(Request httpRequest,
					Response httpResponse) {
				String text = httpResponse.getText();
				RemoteComponentResponse response = text.isEmpty() ? null
						: ReflectiveSerializer.deserialize(text);
				if (response != null) {
					ProtocolMessage message = response.protocolMessage;
					Class<? extends ProtocolMessage> messageClass = request.protocolMessage
							.getClass();
					if (message != null) {
						ProtocolMessageHandlerClient handler = Registry.impl(
								ProtocolMessageHandlerClient.class,
								message.getClass());
						handler.handle(response, message);
					} else {
						Ax.out("Received no-message response for %s",
								NestedNameProvider.get(messageClass));
					}
					if (messageClass == AwaitRemote.class) {
						// continue 'receive loop' with component server
						sendAwaitRemoteMessage();
					}
				}
			}
		};
		try {
			builder.sendRequest(payload, callback);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static void sendAwaitRemoteMessage() {
		send(new ProtocolMessage.AwaitRemote());
	}

	static void send(ProtocolMessage message) {
		send(message, false);
	}

	/**
	 * @param block
	 *            - currently not implemented, but should (could) be for
	 *            client->server event dispatch - FIXME - remcon - for local
	 *            clients, blocking makes sense - for truly remote (inet),
	 *            probably not
	 */
	static void send(ProtocolMessage message, boolean block) {
		RemoteComponentRequest request = RemoteComponentRequest.create();
		request.protocolMessage = message;
		submitRequest(request);
	}
}
