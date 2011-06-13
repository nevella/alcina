package cc.alcina.framework.gwt.client.util;

import com.google.gwt.dom.client.Node;

public interface NodeFromXpathProvider {
	public Node findXpathWithIndexedText(String xpathStr, Node container) ;
}