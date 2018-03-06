package cc.alcina.framework.entity.parser.structured.node;

import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

public class XmlNodeBuilder {
	protected XmlNode relativeTo;

	private String tag;

	private String text;

	private boolean processingInstruction;

	private StringMap attrs = new StringMap();

	protected boolean built;

	private XmlNode builtNode;

	public XmlNodeBuilder() {
	}

	public XmlNodeBuilder(XmlNode relativeTo) {
		this.relativeTo = relativeTo;
	}

	public XmlNode append() {
		return appendTo(relativeTo);
	}

	public XmlNode appendAsFirstChild() {
		XmlNode node = build();
		relativeTo.node.insertBefore(node.node,
				relativeTo.node.getFirstChild());
		relativeTo.children.invalidate();
		return node;
	}

	public XmlNodeBuilder attr(String key, String value) {
		attrs(key, value);
		return this;
	}

	public XmlNodeBuilder attrNumeric(String key, double d) {
		return attr(key, String.valueOf(d));
	}

	public XmlNodeBuilder attrNumeric(String key, int i) {
		return attr(key, String.valueOf(i));
	}

	public XmlNodeBuilder attrs(String... strings) {
		this.attrs.putAll(new StringMap(Arrays.asList(strings)));
		return this;
	}

	public XmlNodeBuilder attrs(StringMap attrs) {
		this.attrs.putAll(attrs);
		return this;
	}

	public XmlNode build() {
		built = true;
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
		builtNode = doc().nodeFor(node);
		return builtNode;
	}

	public XmlNode builtNode() {
		return builtNode;
	}

	public XmlNodeBuilder className(String className) {
		attrs("class", className);
		return this;
	}

	public XmlNode insertAfter() {
		XmlNode node = build();
		relativeTo.relative().insertAfterThis(node);
		return node;
	}

	public XmlNode insertBeforeThis() {
		return insertBefore(relativeTo);
	}

	public XmlNodeBuilder processingInstruction() {
		this.processingInstruction = true;
		return this;
	}

	public XmlNode replaceWith() {
		XmlNode node = build();
		node.children.adoptFrom(relativeTo);
		relativeTo.replaceWith(node);
		return node;
	}

	public XmlNodeBuilder style(String style) {
		String styleBuf = attrs.getOrDefault("style", "");
		if (styleBuf.length() > 0) {
			styleBuf += "; ";
		}
		styleBuf += style;
		attrs("style", styleBuf);
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

	public XmlNodeBuilder text(String string, Object...args) {
		return text(Ax.format(string, args));
	}

	public XmlNode wrap() {
		XmlNode node = build();
		relativeTo.node.getParentNode().insertBefore(node.node,
				relativeTo.node);
		node.node.appendChild(relativeTo.node);
		relativeTo.parent().invalidate();
		return node;
	}

	public XmlNode wrapChildren() {
		XmlNode node = build();
		node.children.adoptFrom(relativeTo);
		relativeTo.children.append(node);
		return node;
	}

	private XmlNode appendTo(XmlNode appendTo) {
		XmlNode node = build();
		appendTo.node.appendChild(node.node);
		appendTo.children.invalidate();
		return node;
	}

	private XmlDoc doc() {
		return relativeTo.doc;
	}

	private XmlNode insertBefore(XmlNode insertBefore) {
		XmlNode node = build();
		insertBefore.node.getParentNode().insertBefore(node.node,
				insertBefore.node);
		insertBefore.parent().invalidate();
		return node;
	}
}
