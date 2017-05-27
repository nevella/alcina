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

	public XmlNode appendAsFirstChild() {
		XmlNode node = generate();
		relativeTo.node.insertBefore(node.node,
				relativeTo.node.getFirstChild());
		relativeTo.children.invalidate();
		return node;
	}

	public XmlNodeBuilder attrs(String... strings) {
		this.attrs.putAll(new StringMap(Arrays.asList(strings)));
		return this;
	}

	public XmlNodeBuilder attrs(StringMap attrs) {
		this.attrs.putAll(attrs);
		return this;
	}

	public XmlNode insertBeforeThis() {
		XmlNode node = generate();
		relativeTo.node.getParentNode().insertBefore(node.node,
				relativeTo.node);
		relativeTo.parent().invalidate();
		return node;
	}

	public XmlNodeBuilder className(String className) {
		attrs("class", className);
		return this;
	}

	public void insertAfter() {
		XmlNode node = generate();
		relativeTo.relative().insertAfterThis(node);
	}

	public XmlNodeBuilder processingInstruction() {
		this.processingInstruction = true;
		return this;
	}

	public XmlNode replaceWith() {
		XmlNode node = generate();
		node.children.adoptFrom(relativeTo);
		relativeTo.replaceWith(node);
		return node;
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

	public XmlNode wrapChildren() {
		XmlNode node = generate();
		node.children.adoptFrom(relativeTo);
		relativeTo.children.append(node);
		return node;
	}

	private XmlDoc doc() {
		return relativeTo.doc;
	}

	private XmlNode generate() {
		Node node = null;
		if (processingInstruction) {
			if (text == null) {
				throw new RuntimeException("no text");
			}
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

	public XmlNodeBuilder attr(String key, String value) {
		attrs(key, value);
		return this;
	}
}
