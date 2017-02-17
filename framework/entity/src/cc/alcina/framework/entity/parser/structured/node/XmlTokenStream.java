package cc.alcina.framework.entity.parser.structured.node;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class XmlTokenStream implements Iterator<XmlNode> {
	private TreeWalker tw;

	private XmlDoc doc;

	public XmlTokenStream(XmlNode node) {
		this.doc = node.doc;
		this.tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(node.node,
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
		while (true) {
			current = next;
			next = tw.nextNode();
			XmlNode xmlNode = doc.nodeFor(current);
			if (skip.contains(xmlNode)) {
			} else {
				return xmlNode;
			}
		}
	}

	public void skipChildren() {
		if (current == null) {
			return;
		}
		skip(doc.nodeFor(current));
	}

	private Set<XmlNode> skip = new LinkedHashSet<>();

	private Node current;

	public void skip(XmlNode node) {
		skip.add(node);
		skip.addAll(node.children.flatten().collect(Collectors.toList()));
	}

	public void dumpAround() {
		for (int idx = 0; idx < 100; idx++) {
			tw.previousNode();
		}
		for (int idx = 0; idx < 200; idx++) {
			XmlNode xmlNode = doc.nodeFor(tw.nextNode());
			System.out.format("%s: %s\n", idx - 100, xmlNode.fullToString());
		}
		for (int idx = 0; idx < 100; idx++) {
			tw.previousNode();
		}
	}
}
