package cc.alcina.framework.servlet.component.romcom.protocol;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.DocumentAttachId;
import com.google.gwt.dom.client.DocumentAttachId.InvokeProxy;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeAttachId;
import com.google.gwt.dom.client.WindowState;
import com.google.gwt.dom.client.WindowState.NodeUiState;
import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.servlet.component.romcom.protocol.OffsetProtocol.OffsetRegistry;

/**
 * Stores the current UI state object from the client, and if possible returns
 * server-&gt:client js query values without the need for server-client
 * communication
 */
public class RemoteWindowState implements InvokeProxy {
	InvokeProxy remoteDelegate = null;

	/*
	 * the last WindowState packet
	 */
	WindowState windowState;

	/*
	 * the incremental offsetRegistry
	 */
	OffsetRegistry offsetRegistry = new OffsetRegistry();

	Logger logger = LoggerFactory.getLogger(getClass());

	public RemoteWindowState(InvokeProxy remoteDelegate) {
		this.remoteDelegate = remoteDelegate;
	}

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

	NodeUiState computeNodeUiState(AttachId attachId) {
		if (!offsetRegistry.containsKey(attachId)) {
			return null;
		}
		NodeUiState fromOffsets = offsetRegistry.computeNodeUiState(attachId);
		return fromOffsets;
	}

	public <T> T invokeSync(NodeAttachId node, String methodName,
			List<Class> argumentTypes, List<?> arguments, List<Flag> flags) {
		if (windowState != null && argumentTypes == null) {
			if (node != null) {
				if (node instanceof DocumentAttachId) {
					node = ((DocumentAttachId) node).documentFor()
							.getDocumentElement().implAccess().attachIdRemote();
				}
				NodeUiState nodeUiState = computeNodeUiState(
						AttachId.forNode(node));
				if (nodeUiState != null) {
					switch (methodName) {
					case "getScrollTop":
						return (T) (Integer) nodeUiState.scrollPos.i2;
					case "getScrollLeft":
						return (T) (Integer) nodeUiState.scrollPos.i1;
					case "getBoundingClientRect":
						return (T) nodeUiState.boundingClientRect;
					case "getClientHeight":
						return (T) (Integer) windowState.clientHeight;
					case "getClientWidth":
						return (T) (Integer) windowState.clientWidth;
					}
				} else {
					switch (methodName) {
					case "getBoundingClientRect":
						logger.debug("get-bcr [not cached] {}",
								node.node().toNameAttachId());
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

	@Override
	public void onWindowState(WindowState windowState) {
		this.windowState = windowState;
		this.offsetRegistry.update(windowState.offsetsDelta);
	}
}
