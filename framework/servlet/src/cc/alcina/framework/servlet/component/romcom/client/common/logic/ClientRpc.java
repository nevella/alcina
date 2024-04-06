package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.List;
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
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/*
 * Nice thing about statics is they *ensure* statelessness
 * 
 * TODO - romcom - handle network issue retry (client and server)
 */
public class ClientRpc {
	static RemoteComponentProtocol.Session session;

	private transient static int clientMessageId;

	static int awaitDelay = 0;

	static RemoteComponentRequest createRequest() {
		RemoteComponentRequest request = new RemoteComponentRequest();
		request.session = session;
		request.requestId = ++clientMessageId;
		return request;
	}

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
		if (message instanceof AwaitRemote) {
			if (acceptorQueue.isEmpty()) {
				acceptorQueue.submit(message);
			}
		} else {
			submitQueue.submit(message);
		}
	}

	static class BrowserDispatchQueue implements TopicListener<Request> {
		List<RemoteComponentRequest> requestQueue = new ArrayList<>();

		void submit(Message message) {
			RemoteComponentRequest request = createRequest();
			request.protocolMessage = message;
			if (message.sync) {
				submitRequest(request, null, null);
			} else {
				requestQueue.add(request);
				maybeDispatch();
			}
		}

		boolean isEmpty() {
			return requestQueue.isEmpty();
		}

		RemoteComponentRequest inFlightComponentRequest = null;

		Request inFlightHttpRequest;

		void maybeDispatch() {
			if (inFlightComponentRequest != null) {
				return;
			}
			if (requestQueue.isEmpty()) {
				return;
			}
			inFlightComponentRequest = requestQueue.remove(0);
			Topic<Request> calledSignal = Topic.create();
			calledSignal.add(this);
			inFlightHttpRequest = submitRequest(inFlightComponentRequest, null,
					calledSignal);
		}

		@Override
		public void topicPublished(Request httpRequest) {
			if (httpRequest == inFlightHttpRequest) {
				maybeEnqueueAwaitRemote();
				inFlightComponentRequest = null;
				maybeDispatch();
			}
		}

		void maybeEnqueueAwaitRemote() {
			if (!(inFlightComponentRequest.protocolMessage instanceof AwaitRemote)) {
				return;
			}
			enqueueWhenVisible();
		}

		void retry() {
			if (Document.get().getVisibilityState().equals("visible")) {
			} else {
				new Timer() {
					@Override
					public void run() {
						retry();
					}
				}.schedule(100);
			}
		}

		void enqueueWhenVisible() {
			if (Document.get().getVisibilityState().equals("visible")) {
				if (awaitDelay == 0) {
					submit(new AwaitRemote());
				} else {
					/*
					 * if the server is reloading or there's a network issue,
					 * don't try too often
					 */
					new Timer() {
						@Override
						public void run() {
							submit(new AwaitRemote());
						}
					}.schedule(awaitDelay);
					awaitDelay = Math.min(1000, awaitDelay + 100);
				}
			} else {
				new Timer() {
					@Override
					public void run() {
						enqueueWhenVisible();
						;
					}
				}.schedule(100);
			}
		}
	}

	static BrowserDispatchQueue submitQueue = new BrowserDispatchQueue();

	static BrowserDispatchQueue acceptorQueue = new BrowserDispatchQueue();

	static int clientServerMessageCounter = 0;

	static Request submitRequest(RemoteComponentRequest request,
			BiConsumer<RemoteComponentRequest, Throwable> errorHandler,
			Topic<Request> calledSignal) {
		request.protocolMessage.messageId = clientServerMessageCounter++;
		String payload = ReflectiveSerializer.serialize(request);
		String path = Window.Location.getPath();
		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, path);
		RequestCallback callback = new RequestCallback() {
			@Override
			public void onError(Request httpRequest, Throwable exception) {
				new ExceptionHandler().accept(request, exception);
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
						try {
							handler.handle(response, message);
						} catch (Throwable e) {
							Ax.out("Exception handling message %s\n"
									+ "================\nSerialized form:\n%s",
									message, text);
							e.printStackTrace();
							/*
							 * FIXME - devex - 0 this can range - but at least
							 * initially includes invalid pathrefs (which are
							 * basically fatal but continue during dev)
							 * 
							 * correction - since client/server are async, and
							 * thus the client's dom can be behind the server's,
							 * it's not necessarily true that invalid pathrefs
							 * *should* be fatal. but handling is complex
							 */
							Window.alert(
									CommonUtils.toSimpleExceptionMessage(e));
						}
					} else {
						Ax.out("Received no-message response for %s",
								NestedName.get(messageClass));
					}
					signalCalled(httpRequest);
				}
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
					awaitDelay++;
					break;
				default:
					RemoteObjectModelComponentState.get().finished = true;
					break;
				}
			}
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
