package cc.alcina.framework.entity.parser.structured.node;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.XmlUtils;

public class XmlDoc extends XmlNode {
	public XmlDoc(Document domDocument) {
		super(null, null);
		this.node = domDocument;
		nodes.put(this.node, this);
		this.doc = this;
	}

	public XmlDoc(String xml) {
		super(null, null);
		loadFromXml(xml);
	}

	private void loadFromXml(String xml) {
		try {
			this.node = XmlUtils.loadDocument(xml);
			nodes.put(this.node, this);
			this.doc = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public String prettyToString() {
		try {
			return XmlUtils.prettyPrintWithDOM3LS(domDoc());
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

	public XmlNode nodeFor(Node domNode) {
		return nodes.get(domNode);
	}

	void register(XmlNode xmlNode) {
	}

	public XmlNode getDocumentElementNode() {
		return nodeFor(domDoc().getDocumentElement());
	}

	public static XmlNode createDocumentElement(String tag) {
		return new XmlDoc(String.format("<%s/>", tag)).getDocumentElementNode();
	}

	public void restoreNamespaces() {
		pushedNamespaceAttrs.forEach(attr -> {
		});
		int debug = 3;
	}

	List<Attr> pushedNamespaceAttrs = new ArrayList<>();

	public void removeNamespaces() {
		NamedNodeMap atts = domDoc().getDocumentElement().getAttributes();
		for (; atts.getLength() > 0;) {
			Attr attr = (Attr) atts.item(0);
			pushedNamespaceAttrs.add(attr);
			atts.removeNamedItem(attr.getName());
		}
		String fullToString = doc.fullToString();
		loadFromXml(fullToString);
	}
}
