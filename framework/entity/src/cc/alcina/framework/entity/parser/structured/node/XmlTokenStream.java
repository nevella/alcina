package cc.alcina.framework.entity.parser.structured.node;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.parser.structured.XmlToken;

public class XmlTokenStream implements Iterator<XmlNode> {
	private TreeWalker tw;

	private XmlDoc doc;

	public XmlTokenStream(XmlDoc doc) {
		this.doc = doc;
		this.tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(doc.node,
				NodeFilter.SHOW_ALL, null, true);
		next();
	}

	Node next = null;

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public XmlNode next() {
		Node current = next;
		next = tw.nextNode();
		return doc.nodeFor(current);
	}

	public void skipChildren() {
		if (next == null) {
			return;
		}
		tw.previousNode();
		boolean found = false;
		while (true) {
			next = tw.nextSibling();
			if (next != null) {
				break;
			}
			Node parentNode = tw.parentNode();
			if (parentNode == null) {
				break;
			}
		}
	}
}
