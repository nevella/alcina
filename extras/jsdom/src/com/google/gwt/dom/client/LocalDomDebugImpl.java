package com.google.gwt.dom.client;

import com.google.gwt.dom.client.ElementRemote.ElementRemoteIndex;
import com.google.gwt.user.client.LocalDomDebug;

import cc.alcina.framework.common.client.util.Ax;

public class LocalDomDebugImpl {
	final static boolean debug = true;

	

	public void log(LocalDomDebug channel, String message, Object... args) {
		if (!debug) {
			return;
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
				String tagName0 = ((ElementRemote) remoteChild).getTagName0();
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
		String localDom = withRemote.local().provideRemoteDomTree();
		ElementRemote parentRemote = elementRemote.getParentElement0();
		ElementRemoteIndex remoteIndex = elementRemote.provideRemoteIndex(true);
		remoteDebug = remoteIndex.getString();
		int debug = 3;
	}

}
