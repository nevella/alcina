package cc.alcina.framework.common.client.dom;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class DomTokenStream implements Iterator<DomNode> {
	private TreeWalker tw;

	private DomDoc doc;

	Node next = null;

	private Set<DomNode> skip = new LinkedHashSet<>();

	private Node current;

	private Node last;

	public DomTokenStream(DomNode node) {
		this.doc = node.doc;
		this.tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(node.node,
				NodeFilter.SHOW_ALL, null, true);
		next();
	}

	public void afterModification() {
		if (last != null) {
			tw.setCurrentNode(last);
			next = tw.nextNode();
		}
	}

	public void dumpAround() {
		for (int idx = 0; idx < 100; idx++) {
			tw.previousNode();
		}
		for (int idx = 0; idx < 200; idx++) {
			DomNode xmlNode = doc.nodeFor(tw.nextNode());
			System.out.format("%s: %s\n", idx - 100, xmlNode.fullToString());
		}
		for (int idx = 0; idx < 100; idx++) {
			tw.previousNode();
		}
	}

	public DomDoc getDoc() {
		return this.doc;
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	@Override
	public DomNode next() {
		while (true) {
			last = current;
			current = next;
			next = tw.nextNode();
			DomNode xmlNode = doc.nodeFor(current);
			if (skip.contains(xmlNode)) {
			} else {
				return xmlNode;
			}
		}
	}

	public void skip(DomNode node) {
		skip.add(node);
		skip.addAll(node.children.flatten().collect(Collectors.toList()));
	}

	public void skipChildren() {
		if (current == null) {
			return;
		}
		skip(doc.nodeFor(current));
	}

	public void skipChildren(Predicate<DomNode> predicate) {
		DomNode node = doc.nodeFor(current);
		skip.addAll(node.children.flatten().filter(predicate)
				.collect(Collectors.toList()));
	}
}
