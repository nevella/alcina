package cc.alcina.framework.gwt.client.util;

import org.w3c.dom.Node;

public interface NodeFromXpathProvider {
	public Node findXpathWithIndexedText(String xpathStr, Node container);
}