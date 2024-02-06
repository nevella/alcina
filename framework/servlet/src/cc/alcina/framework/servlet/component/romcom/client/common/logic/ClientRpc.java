package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/*
 * Nice thing about statics is they *ensure* statelessness
 */
public class ClientRpc {
	static RemoteComponentProtocol.Session session;

	private transient static int clientMessageId;

	static int awaitDelay = 0;

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
		submitRequest(request, null);
	}

	static RemoteComponentRequest createRequest() {
		RemoteComponentRequest request = new RemoteComponentRequest();
		request.session = session;
		request.requestId = ++clientMessageId;
		return request;
	}

	static void send(Message message) {
		send(message, false);
	}

	/**
	 * @param block
	 *            - currently not implemented, but should (could) be for
	 *            client->server event dispatch - FIXME - remcon - for local
	 *            clients, blocking makes sense - for truly remote (inet),
	 *            probably not
	 */
	static void send(Message message, boolean block) {
		RemoteComponentRequest request = createRequest();
		request.protocolMessage = message;
		submitRequest(request);
	}

	static void sendAwaitRemoteMessage() {
		send(new Message.AwaitRemote());
	}

	static void submitRequest(RemoteComponentRequest request,
			BiConsumer<RemoteComponentRequest, Throwable> errorHandler) {
		String payload = ReflectiveSerializer.serialize(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request httpRequest, Throwable exception) {
				new ExceptionHandler().accept(request, exception);
			}

			@Override
			public void onResponseReceived(Request httpRequest,
					Response httpResponse) {
				if (httpResponse.getStatusCode() == 0
						|| httpResponse.getStatusCode() >= 400) {
					onError(httpRequest, new StatusCodeException(httpResponse));
					return;
				}
				String text = httpResponse.getText();
				RemoteComponentResponse response = text.isEmpty() ? null
						: ReflectiveSerializer.deserialize(text);
				if (response != null) {
					// reset delay (successful response)
					awaitDelay = 0;
					Message message = response.protocolMessage;
					Ax.out("dispatching: %s", NestedName.get(message));
					Class<? extends Message> messageClass = request.protocolMessage
							.getClass();
					if (message != null) {
						ProtocolMessageHandlerClient handler = Registry.impl(
								ProtocolMessageHandlerClient.class,
								message.getClass());
						handler.handle(response, message);
					} else {
						Ax.out("Received no-message response for %s",
								NestedName.get(messageClass));
					}
					if (messageClass == AwaitRemote.class
							&& !RemoteObjectModelComponentState
									.get().finished) {
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

	static class ExceptionHandler
			implements BiConsumer<RemoteComponentRequest, Throwable> {
		@Override
		public void accept(RemoteComponentRequest t, Throwable u) {
			if (u instanceof StatusCodeException) {
				int statusCode = ((StatusCodeException) u).httpResponse
						.getStatusCode();
				switch (statusCode) {
				case 0:
				case 404:
					retry();
					break;
				default:
					break;
				}
			}
		}

		void retry() {
			if (Document.get().getVisibilityState().equals("visible")) {
				new Timer() {
					@Override
					public void run() {
						sendAwaitRemoteMessage();
					}
				}.schedule(awaitDelay);
				awaitDelay = Math.min(1000, awaitDelay + 100);
			} else {
				new Timer() {
					@Override
					public void run() {
						retry();
					}
				}.schedule(100);
			}
		}
	}

	static class StatusCodeException extends Exception {
		Response httpResponse;

		StatusCodeException(Response httpResponse) {
			this.httpResponse = httpResponse;
		}
	}
}
