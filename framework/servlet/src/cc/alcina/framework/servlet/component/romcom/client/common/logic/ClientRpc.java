package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.List;
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
			if (!message.sync) {
				// first, try to merge (particularly important for multiple
				// mousemove dispatch)
				RemoteComponentRequest last = Ax.last(requestQueue);
				if (last != null && last.protocolMessage.canMerge(message)) {
					last.protocolMessage.merge(message);
					return;
				}
			}
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

		RemoteComponentRequest queueingComponentRequest = null;

		Request inFlightHttpRequest;

		// this defers dispatch by (heuristic) 10ms to collect related events
		// (e.g. mousedown/up/click)
		void maybeDispatch() {
			if (inFlightComponentRequest != null) {
				return;
			}
			if (requestQueue.isEmpty()) {
				return;
			}
			if (queueingComponentRequest != null) {
				return;
			}
			queueingComponentRequest = requestQueue.get(0);
			new Timer() {
				@Override
				public void run() {
					inFlightComponentRequest = requestQueue.remove(0);
					queueingComponentRequest = null;
					Topic<Request> calledSignal = Topic.create();
					calledSignal.add(BrowserDispatchQueue.this);
					inFlightHttpRequest = submitRequest(
							inFlightComponentRequest, null, calledSignal);
				}
				// TODO - if there's a mousedown, possibly delay a little longer
				// (depending on latency) to get the corresonding up/click
			}.schedule(10);
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
			if (inFlightComponentRequest != null
					&& !(inFlightComponentRequest.protocolMessage instanceof AwaitRemote)) {
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
			if (timerQueued) {
				return;
			}
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
							timerQueued = false;
						}
					}.schedule(awaitDelay);
					timerQueued = true;
					awaitDelay = Math.min(1000, awaitDelay + 100);
				}
			} else {
				new Timer() {
					@Override
					public void run() {
						enqueueWhenVisible();
						timerQueued = false;
					}
				}.schedule(100);
				timerQueued = true;
			}
		}

		boolean timerQueued = false;
	}

	static BrowserDispatchQueue submitQueue = new BrowserDispatchQueue();

	static BrowserDispatchQueue acceptorQueue = new BrowserDispatchQueue();

	static int clientServerMessageCounter = 0;

	static ExceptionHandler exceptionHandler = new ExceptionHandler();

	static Request submitRequest(RemoteComponentRequest request,
			BiConsumer<RemoteComponentRequest, Throwable> errorHandler,
			Topic<Request> calledSignal) {
		request.protocolMessage.messageId = clientServerMessageCounter++;
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
							 * FIXME - devex - 0 - once syncmutations.3 is
							 * stable, this should not occur (ha!)
							 * 
							 * Serious, the romcom client is a bounded piece of
							 * code that just propagates server changes to the
							 * client dom, so all exceptions *should* be
							 * server-only (unless client dom is mashed by an
							 * extension)
							 */
							Window.alert(
									CommonUtils.toSimpleExceptionMessage(e));
						}
					} else {
						// Ax.out("Received no-message response for %s",
						// NestedName.get(messageClass));
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
					setState(State.err_recoverable);
					awaitDelay++;
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
