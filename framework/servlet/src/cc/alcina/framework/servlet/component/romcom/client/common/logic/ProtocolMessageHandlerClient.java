package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.Selection;
import com.google.gwt.dom.client.mutations.SelectionRecord;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.HandlerContext;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.InvalidClientException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ExceptionTransport;

/*
 * FIXME - beans1x5 - package protected
 */
@Registration.NonGenericSubtypes(ProtocolMessageHandlerClient.class)
public abstract class ProtocolMessageHandlerClient<PM extends Message>
		implements Message.Handler<PM> {
	static boolean isClientFinished(Message message) {
		return message instanceof Message.ProcessingException;
	}

	public abstract void handle(HandlerContext handlerContext, PM message);

	public interface HandlerContext {
	}

	public static class EnvironmentInitCompleteHandler extends
			ProtocolMessageHandlerClient<Message.EnvironmentInitComplete> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.EnvironmentInitComplete message) {
			ClientRpc.get().onEnvironmentInitComplete(message);
		}
	}

	public static class ServerDebugProtocolResponseHandler extends
			ProtocolMessageHandlerClient<Message.ServerDebugProtocolResponse> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.ServerDebugProtocolResponse message) {
			ClientUtils.consoleInfo("Server protocol state:");
			ClientUtils.consoleInfo(message.serverState);
		}
	}

	public static class SetCookieServerSideHandler
			extends ProtocolMessageHandlerClient<Message.SetCookieServerSide> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.SetCookieServerSide message) {
			// client-side noop
		}
	}

	public static class InvokeHandler
			extends ProtocolMessageHandlerClient<Message.Invoke> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.Invoke message) {
			AttachId path = message.path;
			Node node = path == null ? null : path.node();
			Message.InvokeResponse responseMessage = new Message.InvokeResponse();
			responseMessage.id = message.id;
			// if the server requested sync, return that in the response (since
			// the protocol is stateless)
			responseMessage.sync = message.sync;
			Object result = null;
			try {
				if (message.methodName != null) {
					Preconditions.checkNotNull(node, Ax
							.format("invoke - target node %s not found", path));
					result = Reflections.at(node).invoke(node,
							message.methodName, message.argumentTypes,
							message.arguments, message.flags);
				} else {
					Result scriptResult = new Result();
					String script = message.javascript;
					invokeJs(scriptResult, message.jsResponseType.name(), node,
							script);
					result = scriptResult.asObject();
				}
				responseMessage.response = result;
			} catch (Throwable e) {
				Window.alert(CommonUtils.toSimpleExceptionMessage(e));
				e.printStackTrace();
				responseMessage.exception = new ExceptionTransport(e);
			}
			ClientRpc.send(responseMessage);
		}

		static class Result {
			NodeJso node;

			String string;

			public Object asObject() {
				if (node != null) {
					return AttachId.forNode(node.node());
				} else if (string != null) {
					return string;
				} else {
					return null;
				}
			}
		}

		static final native void invokeJs(Result scriptResult,
				String responseType, Node node, String script) /*-{
			var arg = node;
			var ret = eval(script);
			switch(responseType){
				case "_void":
				break;
				case "node_jso":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::node = ret;
				break; 
				case "string":
				scriptResult.@cc.alcina.framework.servlet.component.romcom.client.common.logic.ProtocolMessageHandlerClient.InvokeHandler.Result::string = ret;
				break; 
				default:
				throw "unsupported responseType "+responseType;
			}
			}-*/;
	}

	public static class MutationsHandler
			extends ProtocolMessageHandlerClient<Message.Mutations> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.Mutations message) {
			LocalDom.attachIdRepresentations()
					.applyMutations(message.domMutations, true);
			SelectionRecord selectionMutation = message.selectionMutation;
			if (selectionMutation != null) {
				LocalDom.flush();
				selectionMutation.populateNodes();
				Selection selection = Document.get().getSelection();
				if (selectionMutation.anchorNode != null) {
					selection.collapse(selectionMutation.anchorNode,
							selectionMutation.anchorOffset);
				}
				if (selectionMutation.focusNode != null) {
					selection.extend(selectionMutation.focusNode,
							selectionMutation.focusOffset);
				}
			}
			message.eventSystemMutations.forEach(m -> {
				try {
					Element elem = (Element) m.nodeId.node();
					if (m.eventBits == -1) {
						DOM.sinkBitlessEvent(elem, m.eventTypeName);
					} else {
						DOM.sinkEvents(elem, m.eventBits);
					}
					elem.eventListener = new DispatchListener(elem);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			});
			if (message.locationMutation != null) {
				try {
					RemoteObjectModelComponentState
							.get().firingLocationMutation = true;
					History.CONTEXT_REPLACING.runWith(
							() -> History
									.newItem(message.locationMutation.hash),
							message.locationMutation.replace);
				} finally {
					RemoteObjectModelComponentState
							.get().firingLocationMutation = false;
				}
			}
			/*
			 * state changed, emit an update
			 */
			ClientRpc.send(new Message.WindowStateUpdate());
		}

		static class DispatchListener implements EventListener {
			private Element elem;

			public DispatchListener(Element elem) {
				this.elem = elem;
			}

			@Override
			public void onBrowserEvent(Event event) {
				ClientEventDispatch.dispatchEventMessage(event, elem, false);
			}
		}
	}

	public static class ProcessingExceptionHandler
			extends ProtocolMessageHandlerClient<Message.ProcessingException> {
		@Override
		public boolean isHandleOutOfBand() {
			return true;
		}

		@Override
		public void handle(HandlerContext handlerContext,
				Message.ProcessingException message) {
			RemoteObjectModelComponentState.get().finished = true;
			Exception protocolException = message.protocolException;
			String clientMessage = Ax.format(
					"Exception occurred - ui stopped: %s",
					message.exceptionMessage);
			ClientUtils.consoleError(clientMessage);
			ClientUtils.consoleInfo(message.exceptionTrace);
			if (protocolException instanceof InvalidClientException) {
				InvalidClientException invalidClientException = (InvalidClientException) protocolException;
				switch (invalidClientException.action) {
				case REFRESH:
					Window.Location.reload();
					return;
				}
			}
			// FIXME - remcon - prettier?
			Window.alert(clientMessage);
		}
	}

	public static class PersistSettingsHandler
			extends ProtocolMessageHandlerClient<Message.PersistSettings> {
		@Override
		public void handle(HandlerContext handlerContext,
				Message.PersistSettings message) {
			RemoteComponentSettings.setSettings(message.value);
		}
	}
}