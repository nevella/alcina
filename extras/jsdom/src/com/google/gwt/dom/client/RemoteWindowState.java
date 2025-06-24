package com.google.gwt.dom.client;

import java.util.List;

import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy;
import com.google.gwt.dom.client.DomEventContext.NodeUiState;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Stores the current UI state object from the client, and if possible returns
 * server-&gt:client js query values without the need for server-client
 * communication
 */
class RemoteWindowState implements InvokeProxy {
	InvokeProxy remoteDelegate = null;

	public DomEventContext eventContext;

	public void invoke(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
			AsyncCallback<?> callback) {
		if (eventContext != null && node != null
				&& node instanceof DocumentAttachId) {
			switch (methodName) {
			case "setActiveElement":
				if (eventContext.activeElement != null) {
					((Element) eventContext.activeElement.node()).blur();
				}
				return;
			}
		}
		remoteDelegate.invoke(node, methodName, argumentTypes, arguments, flags,
				callback);
	}

	public <T> T invokeSync(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags) {
		if (eventContext != null && argumentTypes == null) {
			if (node != null) {
				NodeUiState nodeUiState = eventContext
						.uiStateFor(node.getAttachId());
				if (nodeUiState != null) {
					switch (methodName) {
					case "getScrollTop":
						return (T) (Integer) nodeUiState.scrollPos.i2;
					case "getBoundingClientRect":
						return (T) nodeUiState.boundingClientRect;
					case "getClientHeight":
					case "getClientWidth":
						break;
					}
				}
				switch (methodName) {
				case "getActiveElement":
					return eventContext.activeElement == null ? null
							: (T) eventContext.activeElement.node();
				}
			}
		}
		// not handled by cached ui state
		return remoteDelegate.invokeSync(node, methodName, argumentTypes,
				arguments, flags);
	}

	public <T> T invokeSync(NodeAttachId node, String methodName) {
		return invokeSync(node, methodName);
	}

	public <T> T invokeScript(Class clazz, String methodName,
			List<Class> argumentTypes, List<?> arguments) {
		return remoteDelegate.invokeScript(clazz, methodName, argumentTypes,
				arguments);
	}
}
