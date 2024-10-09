package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.Objects;
import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/*
 * Nice thing about statics is they *ensure* statelessness
 * 
 * TODO - romcom - handle network issue retry (client and server)
 */
@Registration.Singleton
public class ClientRpc {
	int awaitDelay = 0;

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

	public static ClientRpc get() {
		return Registry.impl(ClientRpc.class);
	}

	static void send(Message message) {
		get().transportLayer.sendMessage(message);
	}

	MessageTransportLayerClient transportLayer = new MessageTransportLayerClient();

	ExceptionHandler exceptionHandler = new ExceptionHandler();

	/*
	 * probably move all this to the transport...
	 */
	static Request submitRequest(RemoteComponentRequest request,
			BiConsumer<RemoteComponentRequest, Throwable> errorHandler,
			Topic<Request> calledSignal) {
		return get().submitRequest0(request, errorHandler, calledSignal);
	}

	Request submitRequest0(RemoteComponentRequest request,
			BiConsumer<RemoteComponentRequest, Throwable> errorHandler,
			Topic<Request> calledSignal) {
		String payload = ReflectiveSerializer.serializeForRpc(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request httpRequest, Throwable exception) {
				exceptionHandler.accept(request, exception);
				signalCalled(httpRequest);
			}

			void signalCalled(Request httpRequest) {
				if (calledSignal != null) {
					calledSignal.publish(httpRequest);
				}
			}

			@Override
			public void onResponseReceived(Request httpRequest,
					Response httpResponse) {
				if (httpResponse.getStatusCode() == 0
						|| httpResponse.getStatusCode() >= 400) {
					onError(httpRequest, new StatusCodeException(httpResponse));
					signalCalled(httpRequest);
					return;
				}
				exceptionHandler.onSuccessReceived();
				String text = httpResponse.getText();
				RemoteComponentResponse response = text.isEmpty() ? null
						: ReflectiveSerializer.deserializeRpc(text);
				// if (response != null) {
				// // reset delay (successful response)
				// awaitDelay = 0;
				// Message message = response.protocolMessage;
				// Class<? extends Message> requestMessageClass =
				// request.protocolMessage
				// .getClass();
				// // Ax.out("[server->client response] #%s :: [client message
				// // :: %s] ==> %s",
				// // response.requestId,
				// // NestedName.get(requestMessageClass),
				// // message == null ? "[null response]"
				// // : NestedName.get(message));
				// if (message != null) {
				// ProtocolMessageHandlerClient handler = Registry.impl(
				// ProtocolMessageHandlerClient.class,
				// message.getClass());
				// try {
				// handler.handle(response, message);
				// } catch (Throwable e) {
				// Ax.out("Exception handling message %s\n"
				// + "================\nSerialized form:\n%s",
				// message, text);
				// e.printStackTrace();
				// /*
				// * FIXME - devex - 0 - once syncmutations.3 is
				// * stable, this should not occur (ha!)
				// *
				// * Serious, the romcom client is a bounded piece of
				// * code that just propagates server changes to the
				// * client dom, so all exceptions *should* be
				// * server-only (unless client dom is mashed by an
				// * extension)
				// */
				// Window.alert(
				// CommonUtils.toSimpleExceptionMessage(e));
				// }
				// } else {
				// // Ax.out("Received no-message response for %s",
				// // NestedName.get(messageClass));
				// }
				// signalCalled(httpRequest);
				// } else {
				// }
			}
		};
		try {
			return builder.sendRequest(payload, callback);
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
					setState(State.err_recoverable);
					get().awaitDelay++;
					break;
				default:
					setState(State.err_finished);
					RemoteObjectModelComponentState.get().finished = true;
					break;
				}
			}
		}

		enum State {
			ok, err_recoverable, err_finished
		}

		State state = State.ok;

		void setState(State state) {
			State old_state = this.state;
			this.state = state;
			if (state != old_state) {
				if (notificationElement != null) {
					notificationElement.removeFromParent();
					notificationElement = null;
				}
				String message = null;
				switch (state) {
				case ok:
					// just remove, as above
					break;
				case err_finished:
					message = "Unrecoverable exception";
					break;
				case err_recoverable:
					message = "Network/host unreachable";
					break;
				default:
					throw new UnsupportedOperationException();
				}
				if (message != null) {
					notificationElement = Document.get()
							.createElement("romcom-notification");
					notificationElement.setTextContent(message);
					Document.get().getBody().appendChild(notificationElement);
					LocalDom.flush();
					String display = notificationElement.implAccess()
							.ensureJsoRemote().getComputedStyle().getDisplay();
					if (Objects.equals(display, "inline")) {
						// not set by the app, add our own
						notificationElement.setAttribute("style",
								"position: absolute; top:5px; left: 5px; padding: 0.5rem 1rem; "
										+ "display: block; background-color: #333; border: solid 1px #ccc; color: #cc5; z-index: 999");
					}
				}
			}
		}

		Element notificationElement;

		void onSuccessReceived() {
			setState(State.ok);
		}
	}

	static class StatusCodeException extends Exception {
		Response httpResponse;

		StatusCodeException(Response httpResponse) {
			this.httpResponse = httpResponse;
		}
	}

	public static void beginAwaitLoop() {
		send(new Message.AwaitRemote());
	}
}
