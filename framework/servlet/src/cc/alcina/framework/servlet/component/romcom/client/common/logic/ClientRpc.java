package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.mutations.SelectionRecord;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.HandlerContext;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.EnvironmentInitComplete;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.HasSelectionMutation;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Mutations;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentRequest;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentResponse;

/*
 * Nice thing about statics is they *ensure* statelessness
 * 
 * TODO - romcom - handle network issue retry (client and server)
 */
@Registration.Singleton
public class ClientRpc implements HandlerContext {
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
		get().prepareMessage(message);
		get().transportLayer.sendMessage(message);
	}

	void prepareMessage(Message message) {
		if (message instanceof HasSelectionMutation) {
			HasSelectionMutation hasSelectionMutation = (HasSelectionMutation) message;
			conditionallyPopulateSelectionMutation(hasSelectionMutation);
		}
	}

	void conditionallyPopulateSelectionMutation(
			HasSelectionMutation hasSelectionMutation) {
		SelectionRecord currentSelectionRecord = Document.get().getSelection()
				.getSelectionRecord();
		if (!Objects.equals(lastSelectionRecord, currentSelectionRecord)) {
			lastSelectionRecord = currentSelectionRecord;
			hasSelectionMutation.setSelectionMutation(currentSelectionRecord);
		}
	}

	SelectionRecord lastSelectionRecord;

	MessageTransportLayerClient transportLayer;

	ExceptionHandler exceptionHandler;

	RemoteComponentUi ui;

	ClientRpc(RemoteComponentUi ui) {
		this.ui = ui;
		transportLayer = new MessageTransportLayerClient();
		exceptionHandler = new ExceptionHandler();
		transportLayer.topicMessageReceived.add(this::onMessageReceived);
	}

	void onMessageReceived(Message message) {
		ProtocolMessageHandlerClient handler = Registry
				.impl(ProtocolMessageHandlerClient.class, message.getClass());
		try {
			handler.handle(this, message);
		} catch (Throwable e) {
			ui.messageStateRouter.onMessageHandlingException(message, e);
		}
	}

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
			}
		};
		try {
			return builder.sendRequest(payload, callback);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	enum ExceptionState {
		ok, err_recoverable, err_finished
	}

	class ExceptionHandler
			implements BiConsumer<RemoteComponentRequest, Throwable> {
		@Override
		public void accept(RemoteComponentRequest t, Throwable u) {
			if (u instanceof StatusCodeException) {
				int statusCode = ((StatusCodeException) u).httpResponse
						.getStatusCode();
				switch (statusCode) {
				case 0:
				case 404:
					setState(ExceptionState.err_recoverable);
					get().awaitDelay++;
					break;
				default:
					setState(ExceptionState.err_finished);
					RemoteObjectModelComponentState.get().finished = true;
					break;
				}
			}
		}

		ExceptionState state = ExceptionState.ok;

		void setState(ExceptionState state) {
			ExceptionState old_state = this.state;
			this.state = state;
			if (state != old_state) {
				ui.messageStateRouter.clearRpcStateMessage();
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
					ui.messageStateRouter.displayRpcStateMessage(message);
				}
			}
		}

		void onSuccessReceived() {
			setState(ExceptionState.ok);
		}
	}

	static class StatusCodeException extends Exception {
		Response httpResponse;

		StatusCodeException(Response httpResponse) {
			this.httpResponse = httpResponse;
		}
	}

	List<MessageToken> getActiveMessages() {
		return transportLayer.sendChannel.snapshotActiveMessages();
	}

	void onEnvironmentInitComplete(EnvironmentInitComplete message) {
		ui.environmentSettings = message.environmentSettings;
		if (ui.environmentSettings.attachRpcDebugMethod) {
			transportLayer.attachRpcDebugMethod();
		}
	}
}
