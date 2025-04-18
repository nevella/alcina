package cc.alcina.framework.common.client.dom;

import java.util.Arrays;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public class DomNodeBuilder {
	public static final transient String CONTEXT_TEXT_UNESCAPED = DomNodeBuilder.class
			.getName() + ".CONTEXT_TEXT_UNESCAPED";

	protected DomNode relativeTo;

	private String tag;

	private String text;

	private boolean processingInstruction;

	private boolean cdata;

	private StringMap attrs = new StringMap();

	protected boolean built;

	private DomNode builtNode;

	public DomNodeBuilder() {
	}

	public DomNodeBuilder(DomNode relativeTo) {
		this.relativeTo = relativeTo;
	}

	public DomNode append() {
		return appendTo(relativeTo);
	}

	private DomNode appendTo(DomNode appendTo) {
		DomNode node = build();
		appendTo.node.appendChild(node.node);
		return node;
	}

	public DomNodeBuilder attr(String key, String value) {
		if (Ax.notBlank(value)) {
			attrs(key, value);
		}
		return this;
	}

	public DomNodeBuilder attrNumeric(String key, double d) {
		return attr(key, String.valueOf(d));
	}

	public DomNodeBuilder attrNumeric(String key, int i) {
		return attr(key, String.valueOf(i));
	}

	public DomNodeBuilder attrs(String... strings) {
		this.attrs.putAll(new StringMap(Arrays.asList(strings)));
		return this;
	}

	public DomNodeBuilder attrs(StringMap attrs) {
		this.attrs.putAll(attrs);
		return this;
	}

	public DomNode build() {
		built = true;
		Node node = null;
		if (processingInstruction) {
			if (text == null) {
				throw new RuntimeException("no text");
			}
			node = doc().w3cDoc().createProcessingInstruction(tag, text);
		} else if (cdata) {
			if (text == null) {
				throw new RuntimeException("no text");
			}
			node = doc().w3cDoc().createCDATASection(text);
		} else if (tag != null) {
			node = doc().w3cDoc().createElement(tag);
			if (text != null) {
				if (LooseContext.is(CONTEXT_TEXT_UNESCAPED)
						&& text.startsWith(CommonUtils.XML_PI)) {
					builtNode = doc().nodeFor(node);
					builtNode.setInnerXml(text);
				} else {
					node.setTextContent(text);
				}
			}
			Node f_node = node;
			attrs.forEach((k, v) -> {
				if (Ax.notBlank(v)) {
					((Element) f_node).setAttribute(k, v);
				}
			});
		} else {
			node = doc().w3cDoc().createTextNode(text);
		}
		builtNode = doc().nodeFor(node);
		return builtNode;
	}

	public DomNode builtNode() {
		return builtNode;
	}

	public DomNodeBuilder className(String className) {
		attrs("class", className);
		return this;
	}

	private DomDocument doc() {
		return relativeTo.document;
	}

	public DomNode getRelativeTo() {
		return this.relativeTo;
	}

	public DomNode insertAfterThis() {
		DomNode node = build();
		relativeTo.relative().insertAfterThis(node);
		return node;
	}

	public DomNode insertAsFirstChild() {
		DomNode node = build();
		relativeTo.node.insertBefore(node.node,
				relativeTo.node.getFirstChild());
		return node;
	}

	private DomNode insertBefore(DomNode insertBefore) {
		DomNode node = build();
		insertBefore.node.getParentNode().insertBefore(node.node,
				insertBefore.node);
		return node;
	}

	public DomNode insertBeforeThis() {
		return insertBefore(relativeTo);
	}

	public DomNodeBuilder processingInstruction() {
		this.processingInstruction = true;
		return this;
	}

	public DomNode replaceWith() {
		DomNode node = build();
		if (node.isElement()) {
			node.children.adoptFrom(relativeTo);
		}
		relativeTo.replaceWith(node);
		return node;
	}

	public DomNodeBuilder style(String style) {
		String styleBuf = attrs.getOrDefault("style", "");
		if (styleBuf.length() > 0) {
			styleBuf += "; ";
		}
		styleBuf += style;
		attrs("style", styleBuf);
		return this;
	}

	public DomNodeBuilder tag(String tag) {
		this.tag = tag;
		return this;
	}

	public DomNodeBuilder text(String text) {
		this.text = text;
		return this;
	}

	public DomNodeBuilder text(String string, Object... args) {
		return text(Ax.format(string, args));
	}

	public DomNodeBuilder title(String title) {
		if (title.isEmpty()) {
			this.attrs.remove("title");
		} else {
			attrs("title", title);
		}
		return this;
	}

	public DomNode wrap() {
		DomNode node = build();
		relativeTo.node.getParentNode().insertBefore(node.node,
				relativeTo.node);
		node.node.appendChild(relativeTo.node);
		return node;
	}

	public DomNode wrapChildren() {
		DomNode node = build();
		node.children.adoptFrom(relativeTo);
		relativeTo.children.append(node);
		return node;
	}

	public DomNodeBuilder cdata() {
		this.cdata = true;
		return this;
	}
}
