package cc.alcina.framework.gwt.client.dirndl.layout;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;

class RenderedW3cNode implements Rendered {
	private Node node;

	RenderedW3cNode(Node node) {
		this.node = node;
	}

	@Override
	public void append(Rendered rendered) {
		node.appendChild(rendered.getNode());
	}

	@Override
	public void appendToRoot() {
		Registry.impl(RootModifier.class).appendToRoot(this);
	}

	@Override
	public <T> T as(Class<T> clazz) {
		if (clazz == Element.class) {
			return (T) node;
		} else if (clazz == Widget.class) {
			return (T) new UnstructuredChild(asElement());
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public Element asElement() {
		return as(Element.class);
	}

	@Override
	public int getChildCount() {
		return node.getChildNodes().getLength();
	}

	@Override
	public int getChildIndex(Rendered rendered) {
		NodeList childNodes = node.getChildNodes();
		int length = childNodes.getLength();
		for (int idx = 0; idx < length; idx++) {
			Node item = childNodes.item(idx);
			if (item == rendered.getNode()) {
				return idx;
			}
		}
		return -1;
	}

	@Override
	public Node getNode() {
		return node;
	}

	@Override
	public void insertAsFirstChild(Rendered rendered) {
		node.insertBefore(rendered.getNode(), node.getFirstChild());
	}

	@Override
	public void insertChild(Rendered rendered, int idx) {
		NodeList childNodes = node.getChildNodes();
		if (idx == childNodes.getLength()) {
			node.appendChild(rendered.getNode());
		} else {
			Node before = childNodes.item(idx);
			node.insertBefore(rendered.getNode(), before);
		}
	}

	@Override
	public boolean isElement() {
		return node instanceof Element;
	}

	@Override
	public void removeFromParent() {
		node.getParentNode().removeChild(node);
	}

	static class UnstructuredChild extends Widget {
		public UnstructuredChild(Element element) {
			setElement(element);
		}
	}
}