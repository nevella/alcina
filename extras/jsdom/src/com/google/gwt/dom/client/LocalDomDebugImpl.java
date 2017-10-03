package com.google.gwt.dom.client;

import java.util.List;

import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.util.Ax;

public class LocalDomDebugImpl {
	final static boolean debug = true;

	static boolean debugAll = false;

	public void log(LocalDomDebug channel, String message, Object... args) {
		if (!debug) {
			return;
		}
		if (!debugAll) {
			switch (channel) {
			case FLUSH:
			case DOM_EVENT:
			case REQUIRES_SYNC:
			case CREATED_PENDING_RESOLUTION:
			case DISPATCH_DETAILS:
			case DOM_MOUSE_EVENT:
			case STYLE:
				return;
			}
		}
		if (args.length > 0) {
			message = Ax.format(message, args);
		}
		Ax.out("%s: %s", channel, message);
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
			if (remoteChild.provideIsElement()) {
				localChild = withRemote.getChild(idx);
				String tagName0 = ((ElementRemote) remoteChild)
						.getTagNameInternal();
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

	private void debugElement(Element element, Element withRemote,
			ElementRemote elementRemote, NodeRemote remoteChild,
			Node localChild, String issue) {
		String remoteDebug = null;
		String innerHTML = element.getInnerHTML();
		String remoteDom = elementRemote.provideRemoteDomTree();
		String localDom = withRemote.local().provideLocalDomTree();
		ElementRemote parentRemote = elementRemote.getParentElement0();
		ElementRemoteIndex remoteIndex = elementRemote.provideRemoteIndex(true);
		remoteDebug = remoteIndex.getString();
		log(LocalDomDebug.DEBUG_ISSUE, issue);
		int debug = 3;
	}

	public void debugNodeFor0(ElementRemote elementRemote, Element hasNode,
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
		String remoteDom = elementRemote.provideRemoteDomTree();
		String localDom = hasNode.local().provideLocalDomTree();
		ElementRemote parentRemote = elementRemote.getParentElement0();
		remoteDebug = remoteIndex.getString();
		log(LocalDomDebug.DEBUG_ISSUE, "mismatched sizes");
		int debug = 3;
	}
}
