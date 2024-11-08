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
class RemoteUiState implements InvokeProxy {
	InvokeProxy remoteDelegate = null;

	public DomEventContext eventContext;

	public void invoke(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
			AsyncCallback<?> callback) {
		remoteDelegate.invoke(node, methodName, argumentTypes, arguments, flags,
				callback);
	}

	public <T> T invokeSync(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags) {
		if (eventContext != null && node != null && argumentTypes == null) {
			NodeUiState nodeUiState = eventContext
					.uiStateFor(node.getAttachId());
			if (nodeUiState != null) {
				switch (methodName) {
				case "getScrollTop":
					return (T) (Integer) nodeUiState.scrollPos.i2;
				case "getBoundingClientRect":
					return (T) nodeUiState.boundingClientRect;
				}
			}
		}
		int debug = 3;
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
