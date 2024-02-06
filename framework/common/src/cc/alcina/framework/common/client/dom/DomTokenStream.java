package cc.alcina.framework.common.client.dom;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

/**
 * <p>
 * Ideally the treewalker would be positioned before 'current' - since it isn't,
 * we treat the first (root) node specially.
 *
 * <p>
 * Implementation is optimised to have only one TreeWalker.next() per next()
 * (excluding skips) and to avoid DomDocument.nodeFor() calls
 */
public class DomTokenStream implements Iterator<DomNode> {
	private TreeWalker tw;

	private DomDocument doc;

	private Set<Node> skip = new LinkedHashSet<>();

	private Node hasNextFor;

	private Node next;

	private boolean rootIterated;

	public DomTokenStream(DomNode node) {
		this.doc = node.document;
		this.tw = ((DocumentTraversal) doc.domDoc()).createTreeWalker(
				node.w3cNode(), NodeFilter.SHOW_ALL, null, true);
	}

	public void afterModification() {
		hasNextFor = null;
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

	public DomDocument getDoc() {
		return this.doc;
	}

	@Override
	public boolean hasNext() {
		Node currentNode = tw.getCurrentNode();
		if (currentNode != hasNextFor) {
			hasNextFor = currentNode;
			next = nextWithSkip();
			if (next != null) {
				tw.setCurrentNode(currentNode);
			} else {
			}
		}
		return next != null;
	}

	@Override
	public DomNode next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		} else {
			if (!rootIterated) {
				rootIterated = true;
				// clear, since this will equal root, and we need to recalc next
				hasNextFor = null;
			}
			tw.setCurrentNode(next);
			return doc.nodeFor(tw.getCurrentNode());
		}
	}

	private Node nextWithSkip() {
		if (!rootIterated) {
			Node currentNode = tw.getCurrentNode();
			if (skip.contains(currentNode)) {
				return null;
			} else {
				return currentNode;
			}
		}
		while (true) {
			// will be null at the end of the traversal
			Node next = tw.nextNode();
			if (skip.contains(next)) {
				// never true at the end of the traversal
			} else {
				return next;
			}
		}
	}

	public void skip(DomNode node) {
		node.stream().map(DomNode::w3cNode).forEach(skip::add);
	}

	public void skipChildren() {
		DomNode currentDomNode = doc.nodeFor(tw.getCurrentNode());
		currentDomNode.descendants().map(DomNode::w3cNode)
				.forEach(skip::add);
	}

	public void skipChildren(Predicate<DomNode> predicate) {
		DomNode currentDomNode = doc.nodeFor(tw.getCurrentNode());
		currentDomNode.descendants().filter(predicate).map(DomNode::w3cNode)
				.forEach(skip::add);
	}
}
