package cc.alcina.framework.entity.parser.structured.node;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.parser.structured.XmlTokenNode;

public class XmlDoc extends XmlNode {
	public XmlDoc(Document domDocument) {
		super(null, null);
		this.node = domDocument;
		nodes.put(this.node, this);
		this.doc = this;
	}

	public XmlDoc(String xml) {
		super(null, null);
		try {
			this.node = XmlUtils.loadDocument(xml);
			nodes.put(this.node, this);
			this.doc = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Document domDoc() {
		return super.domDoc();
	}

	public XmlNode root() {
		return nodeFor(domDoc().getDocumentElement());
	}

	private CachingMap<Node, XmlNode> nodes = new CachingMap<Node, XmlNode>(
			n -> n == null ? null : new XmlNode(n, this));

	protected XmlNode nodeFor(Node domNode) {
		return nodes.get(domNode);
	}

	void register(XmlNode xmlNode) {
	}
}
