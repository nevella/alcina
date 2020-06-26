package com.google.gwt.dom.client;

import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.user.client.LocalDomDebug;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class LocalDomDebugImpl {
	final static boolean debug = true;

	static boolean debugAll = false;

	public void debugNodeFor(ElementRemote elementRemote, Element hasNode,
			ElementRemoteIndex remoteIndex, boolean firstPass) {
		if (GWT.isScript()) {
		} else {
			debugNodeFor0(elementRemote, hasNode, remoteIndex, firstPass);
		}
	}

	public void debugPutRemote(Element needsRemote, int idx,
			Element withRemote) {
		if (!debug) {
			return;
		}
		NodeListRemote<Node> childNodes = withRemote.typedRemote()
				.getChildNodes0();
		NodeRemote remoteChild = null;
		Node localChild = null;
		String issue = null;
		if (childNodes.getLength() != withRemote.getChildCount()) {
			int length = childNodes.getLength();
			issue = "mismatched child node counts";
		}
		if (issue == null) {
			remoteChild = childNodes.getItem0(idx);
			if (remoteChild == null) {
				issue = "node removed";
			} else if (remoteChild.provideIsElement()) {
				localChild = withRemote.getChild(idx);
				String tagName0 = ((ElementRemote) remoteChild)
						.getTagNameRemote();
				if (!tagName0.equalsIgnoreCase(localChild.getNodeName())) {
					issue = "mismatched tagname";
				}
			}
		}
		if (issue != null) {
			debugElement(needsRemote, withRemote, withRemote.typedRemote(),
					remoteChild, localChild, issue);
		}
	}

	public void log(LocalDomDebug channel, String message, Object... args) {
		if (!debug) {
			return;
		}
		if (!debugAll) {
			switch (channel) {
			case RESOLVE:
			case DOM_EVENT:
			case REQUIRES_SYNC:
			case CREATED_PENDING_RESOLUTION:
			case DISPATCH_DETAILS:
			case DOM_MOUSE_EVENT:
			case STYLE:
			case EVENT_MOD:
				return;
			}
		}
		if (args.length > 0) {
			message = Ax.format(message, args);
		}
		Ax.out("%s: %s", channel, message);
		if (channel == LocalDomDebug.DEBUG_ISSUE
				&& Window.Location.getPort().contains("8080")) {
			// throw new RuntimeException();
			GWT.log("localdom issue", new RuntimeException());
		}
	}

	private void debugElement(Element element, Element withRemote,
			ElementRemote elementRemote, NodeRemote remoteChild,
			Node localChild, String issue) {
		String remoteDebug = null;
		String innerHTML = element.getInnerHTML();
		String remoteDom = elementRemote.provideRemoteDomTree();
		String localDom = withRemote.local().provideLocalDomTree();
		ElementRemote parentRemote = elementRemote.getParentElementRemote();
		ElementRemoteIndex remoteIndex = elementRemote.provideRemoteIndex(true);
		remoteDebug = remoteIndex.getString();
		log(LocalDomDebug.DEBUG_ISSUE, issue);
		int debug = 3;
	}

	private void debugNodeFor0(ElementRemote elementRemote, Element hasNode,
			ElementRemoteIndex remoteIndex, boolean firstPass) {
		if (remoteIndex.hasRemoteDefined() && firstPass) {
			return;
		}
		List<Integer> sizes = remoteIndex.sizes();
		List<Integer> indicies = remoteIndex.indicies();
		boolean sizesMatch = true;
		Element cursor = hasNode;
		for (int idx = sizes.size() - 1; idx >= 0; idx--) {
			int size = sizes.get(idx);
			if (cursor.getChildCount() != size) {
				sizesMatch = false;
				break;
			}
			int nodeIndex = indicies.get(idx);
			cursor = (Element) cursor.getChild(nodeIndex);
		}
		if (sizesMatch) {
			return;
		}
		remoteIndex = elementRemote.provideRemoteIndex(false);
		String remoteDebug = null;
		String remoteDomHasNode = hasNode.typedRemote().provideRemoteDomTree();
		String remoteDom = elementRemote.provideRemoteDomTree();
		String localDomHasNode = hasNode.local().provideLocalDomTree();
		String remoteDomHasNode2 = CommonUtils
				.trimLinesToChars(remoteDomHasNode, 50);
		String localDomHasNode2 = CommonUtils.trimLinesToChars(localDomHasNode,
				50);
		ElementRemote parentRemote = elementRemote.getParentElementRemote();
		remoteDebug = remoteIndex.getString();
		String hashes = Ax.format("%s: %s %s %s", hasNode.getTagName(),
				hasNode.hashCode(), hasNode.local().hashCode(),
				hasNode.remote().hashCode());
		LocalDom.debug(hasNode.typedRemote());
		log(LocalDomDebug.DEBUG_ISSUE, "mismatched sizes");
		int debug = 3;
	}
}
