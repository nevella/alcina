package cc.alcina.framework.entity.parser.structured.node;

import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.util.StringMap;

public class XmlNodeBuilder {
	private XmlNode relativeTo;

	private String tag;

	private String text;

	private boolean processingInstruction;

	private StringMap attrs = new StringMap();

	public XmlNodeBuilder() {
	}

	public XmlNodeBuilder(XmlNode relativeTo) {
		this.relativeTo = relativeTo;
	}

	public XmlNode append() {
		XmlNode node = generate();
		relativeTo.node.appendChild(node.node);
		relativeTo.children.invalidate();
		return node;
	}

	public XmlNodeBuilder attrs(String... strings) {
		this.attrs = new StringMap(Arrays.asList(strings));
		return this;
	}

	public XmlNodeBuilder attrs(StringMap attrs) {
		this.attrs = attrs;
		return this;
	}

	public XmlNode before() {
		XmlNode node = generate();
		relativeTo.node.getParentNode().insertBefore(node.node,
				relativeTo.node);
		relativeTo.parent().invalidate();
		return node;
	}

	public XmlNodeBuilder processingInstruction() {
		this.processingInstruction = true;
		return this;
	}

	public XmlNodeBuilder tag(String tag) {
		this.tag = tag;
		return this;
	}

	public XmlNodeBuilder text(String text) {
		this.text = text;
		return this;
	}

	public XmlNode wrap() {
		XmlNode node = generate();
		relativeTo.node.getParentNode().insertBefore(node.node,
				relativeTo.node);
		node.node.appendChild(relativeTo.node);
		relativeTo.parent().invalidate();
		return node;
	}

	private XmlDoc doc() {
		return relativeTo.doc;
	}

	private XmlNode generate() {
		Node node = null;
		if (processingInstruction) {
			node = doc().domDoc().createProcessingInstruction(tag, text);
		} else if (tag != null) {
			node = doc().domDoc().createElement(tag);
			if (text != null) {
				node.setTextContent(text);
			}
			Node f_node = node;
			attrs.forEach((k, v) -> ((Element) f_node).setAttribute(k, v));
		} else {
			node = doc().domDoc().createTextNode(text);
		}
		return doc().nodeFor(node);
	}
}
