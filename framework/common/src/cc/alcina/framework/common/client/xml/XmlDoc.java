package cc.alcina.framework.common.client.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.xml.XmlEnvironment.NamespaceResult;

public class XmlDoc extends XmlNode {
	public static XmlDoc basicHtmlDoc() {
		return new XmlDoc("<html><head></head><body></body></html>");
	}

	public static XmlNode createDocumentElement(String tag) {
		return new XmlDoc(Ax.format("<%s/>", tag)).getDocumentElementNode();
	}

	private CachingMap<Node, XmlNode> nodes = new CachingMap<Node, XmlNode>(
			n -> n == null ? null : new XmlNode(n, this));

	private String firstTag;

	private boolean readonly;

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

	@Override
	public Document domDoc() {
		return super.domDoc();
	}

	public XmlNode getDocumentElementNode() {
		return nodeFor(domDoc().getDocumentElement());
	}

	public boolean isReadonly() {
		return this.readonly;
	}

	public XmlNode nodeFor(Node domNode) {
		return nodes.get(domNode);
	}

	@Override
	public String prettyToString() {
		try {
			return XmlEnvironment.get().prettyPrint(domDoc());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void removeNamespaces() {
		NamespaceResult namespaceResult = XmlEnvironment.get()
				.removeNamespaces(this);
		firstTag = namespaceResult.firstTag;
		loadFromXml(namespaceResult.xml);
	}

	public void restoreNamespaces() {
		NamespaceResult namespaceResult = XmlEnvironment.get()
				.restoreNamespaces(this, firstTag);
		loadFromXml(namespaceResult.xml);
	}

	public XmlNode root() {
		return nodeFor(domDoc().getDocumentElement());
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	private void loadFromXml(String xml) {
		try {
			this.node = XmlEnvironment.get().loadFromXml(xml);
			nodes.put(this.node, this);
			this.doc = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void register(XmlNode xmlNode) {
	}

	@RegistryLocation(registryPoint = XmlReadonlyDocCache.class, implementationType = ImplementationType.SINGLETON)
	public static class XmlReadonlyDocCache {
		public static XmlDoc.XmlReadonlyDocCache get() {
			return Registry.impl(XmlDoc.XmlReadonlyDocCache.class);
		}

		private int maxSize = 0;

		private Map<String, XmlDoc> docs = new LinkedHashMap<String, XmlDoc>() {
			@Override
			protected boolean
					removeEldestEntry(Map.Entry<String, XmlDoc> eldest) {
				return size() > maxSize;
			};
		};

		int missCount = 0;

		int hitCount = 0;

		public synchronized XmlDoc get(String xml) {
			XmlDoc doc = docs.get(xml);
			if (doc == null) {
				doc = new XmlDoc(xml);
				doc.setReadonly(true);
				docs.put(xml, doc);
				missCount++;
			} else {
				hitCount++;
			}
			return doc;
		}

		public int getMaxSize() {
			return this.maxSize;
		}

		public void setMaxSize(int maxSize) {
			this.maxSize = maxSize;
		}
	}
}
