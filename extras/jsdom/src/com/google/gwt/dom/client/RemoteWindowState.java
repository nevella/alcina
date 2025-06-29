package com.google.gwt.dom.client;

import java.util.List;

import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy;
import com.google.gwt.dom.client.WindowState.NodeUiState;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.Ax;

/**
 * Stores the current UI state object from the client, and if possible returns
 * server-&gt:client js query values without the need for server-client
 * communication
 */
class RemoteWindowState implements InvokeProxy {
	InvokeProxy remoteDelegate = null;

	WindowState windowState;

	public void invoke(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags,
			AsyncCallback<?> callback) {
		if (windowState != null && node != null
				&& node instanceof DocumentAttachId) {
			switch (methodName) {
			case "setActiveElement":
				if (windowState.activeElement != null) {
					((Element) windowState.activeElement.node()).blur();
				}
				return;
			}
		}
		remoteDelegate.invoke(node, methodName, argumentTypes, arguments, flags,
				callback);
	}

	public <T> T invokeSync(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags) {
		if (windowState != null && argumentTypes == null) {
			if (node != null) {
				NodeUiState nodeUiState = windowState
						.uiStateFor(node.getAttachId());
				if (nodeUiState != null) {
					switch (methodName) {
					case "getScrollTop":
						return (T) (Integer) nodeUiState.scrollPos.i2;
					case "getBoundingClientRect":
						Ax.out("bcr: %s", node.node().toNameAttachId());
						return (T) nodeUiState.boundingClientRect;
					case "getClientHeight":
					case "getClientWidth":
						break;
					}
				} else {
					switch (methodName) {
					case "getBoundingClientRect":
						if (node.node().asDomNode().nameIs("p")) {
							Object result = remoteDelegate.invokeSync(node,
									methodName, argumentTypes, arguments,
									flags);
							int debug = 3;
						}
						Ax.out("bcr: [miss] %s", node.node().toNameAttachId());
						break;
					}
				}
				switch (methodName) {
				case "getActiveElement":
					return windowState.activeElement == null ? null
							: (T) windowState.activeElement.node();
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
