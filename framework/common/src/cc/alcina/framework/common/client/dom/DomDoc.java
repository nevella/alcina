package cc.alcina.framework.common.client.dom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomEnvironment.NamespaceResult;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;

public class DomDoc extends DomNode {
	public static DomDoc basicHtmlDoc() {
		return new DomDoc("<html><head></head><body></body></html>");
	}

	public static DomNode createDocumentElement(String tag) {
		return new DomDoc(Ax.format("<%s/>", tag)).getDocumentElementNode();
	}

	public static DomDoc from(Document domDocument) {
		return new DomDoc(domDocument);
	}

	public static DomDoc from(String xml) {
		return new DomDoc(xml);
	}

	private CachingMap<Node, DomNode> nodes = new CachingMap<Node, DomNode>(
			n -> n == null ? null : new DomNode(n, this));

	private String firstTag;

	private boolean readonly;

	private boolean useCachedElementIds;

	private Map<String, Element> cachedElementIdMap;

	public DomDoc(Document domDocument) {
		super(null, null);
		this.node = domDocument;
		nodes.put(this.node, this);
		this.doc = this;
	}

	public DomDoc(String xml) {
		super(null, null);
		loadFromXml(xml);
	}

	public void clearElementReferences() {
		if (cachedElementIdMap != null) {
			cachedElementIdMap.clear();
		}
		nodes.clear();
	}

	@Override
	public Document domDoc() {
		return super.domDoc();
	}

	public DomNode getDocumentElementNode() {
		return nodeFor(domDoc().getDocumentElement());
	}

	public Element getElementById(String elementId) {
		if (useCachedElementIds) {
			if (cachedElementIdMap == null) {
				cachedElementIdMap = new LinkedHashMap<String, Element>();
				Stack<Element> elts = new Stack<Element>();
				elts.push(((Document) node).getDocumentElement());
				while (!elts.isEmpty()) {
					Element elt = elts.pop();
					if (elt.hasAttribute("id")) {
						cachedElementIdMap.put(elt.getAttribute("id"), elt);
					}
					int length = elt.getChildNodes().getLength();
					for (int idx = 0; idx < length; idx++) {
						Node node = elt.getChildNodes().item(idx);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							elts.push((Element) node);
						}
					}
				}
			}
			return cachedElementIdMap.get(elementId);
		} else {
			/*
			 * Probably should throw an exception
			 */
			if (GWT.isClient()) {
				return ((Document) node).getElementById(elementId);
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}

	public boolean isReadonly() {
		return this.readonly;
	}

	public DomNode nodeFor(Node domNode) {
		return nodes.get(domNode);
	}

	@Override
	public String prettyToString() {
		try {
			return DomEnvironment.get().prettyPrint(domDoc());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void removeNamespaces() {
		NamespaceResult namespaceResult = DomEnvironment.get()
				.removeNamespaces(this);
		firstTag = namespaceResult.firstTag;
		loadFromXml(namespaceResult.xml);
	}

	public void restoreNamespaces() {
		NamespaceResult namespaceResult = DomEnvironment.get()
				.restoreNamespaces(this, firstTag);
		loadFromXml(namespaceResult.xml);
	}

	public DomNode root() {
		return nodeFor(domDoc().getDocumentElement());
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public DomDoc withUseCachedElementIds(boolean useCachedElementIds) {
		this.useCachedElementIds = useCachedElementIds;
		return this;
	}

	private void loadFromXml(String xml) {
		try {
			this.node = DomEnvironment.get().loadFromXml(xml);
			nodes.put(this.node, this);
			this.doc = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void register(DomNode xmlNode) {
	}

	@RegistryLocation(registryPoint = XmlReadonlyDocCache.class, implementationType = ImplementationType.SINGLETON)
	public static class XmlReadonlyDocCache {
		public static DomDoc.XmlReadonlyDocCache get() {
			return Registry.impl(DomDoc.XmlReadonlyDocCache.class);
		}

		private int maxSize = 0;

		private Map<String, DomDoc> docs = new LinkedHashMap<String, DomDoc>() {
			@Override
			protected boolean
					removeEldestEntry(Map.Entry<String, DomDoc> eldest) {
				return size() > maxSize;
			};
		};

		int missCount = 0;

		int hitCount = 0;

		public synchronized DomDoc get(String xml) {
			DomDoc doc = docs.get(xml);
			if (doc == null) {
				doc = new DomDoc(xml);
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
