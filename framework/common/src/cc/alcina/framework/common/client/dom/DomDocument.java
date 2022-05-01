package cc.alcina.framework.common.client.dom;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomEnvironment.NamespaceResult;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Multimap;

public class DomDocument extends DomNode {
	private static transient PerDocumentSupplier perDocumentSupplier = Registry
			.impl(PerDocumentSupplier.class);

	public static DomDocument basicHtmlDoc() {
		return new DomDocument("<html><head></head><body></body></html>");
	}

	public static DomNode createDocumentElement(String tag) {
		return new DomDocument(Ax.format("<%s/>", tag))
				.getDocumentElementNode();
	}

	public static DomDocument documentFor(Document document) {
		return perDocumentSupplier.get(document);
	}

	public static DomDocument from(String xml) {
		return new DomDocument(xml);
	}

	private Map<Node, DomNode> nodes;

	private String firstTag;

	private boolean readonly;

	private boolean useCachedElementIds;

	private Map<String, Element> cachedElementIdMap;

	private Multimap<String, List<DomNode>> byTag;

	private Multimap<String, List<DomNode>> byId;

	public DomDocument(Document w3cDocument) {
		this(w3cDocument, 0);
	}

	public DomDocument(Document domDocument, int contentLength) {
		super(null, null);
		initNodes(contentLength);
		this.node = domDocument;
		nodes.put(this.node, this);
		this.document = this;
	}

	public DomDocument(String xml) {
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
		return nodes.computeIfAbsent(domNode,
				dn -> dn == null ? null : new DomNode(domNode, this));
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

	public DomDocument withUseCachedElementIds(boolean useCachedElementIds) {
		this.useCachedElementIds = useCachedElementIds;
		return this;
	}

	private void ensureByLookups() {
		if (byTag == null) {
			byTag = new Multimap<>();
			byId = new Multimap<>();
			byTag = getDocumentElementNode().children.stream()
					.collect(AlcinaCollectors.toKeyMultimap(DomNode::name));
			byId = getDocumentElementNode().children.stream()
					.filter(n -> n.has("id"))
					.collect(AlcinaCollectors.toKeyMultimap(n -> n.attr("id")));
		}
	}

	private void initNodes(int length) {
		nodes = CollectionCreators.Bootstrap.getHashMapCreator()
				.create(length / 20);
	}

	private void loadFromXml(String xml) {
		initNodes(xml.length());
		try {
			this.node = DomEnvironment.get().loadFromXml(xml);
			nodes.put(this.node, this);
			this.document = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	Multimap<String, List<DomNode>> byId() {
		ensureByLookups();
		return byId;
	}

	Multimap<String, List<DomNode>> byTag() {
		ensureByLookups();
		return byTag;
	}

	void register(DomNode xmlNode) {
	}

	@Reflected
	@Registration.Singleton
	public static class PerDocumentSupplier {
		private Map<Document, DomDocument> perDocument;

		public PerDocumentSupplier() {
			perDocument = new LinkedHashMap<>();
		}

		public DomDocument get(Document document) {
			synchronized (perDocument) {
				return perDocument.computeIfAbsent(document, DomDocument::new);
			}
		}
	}

	@Registration.Singleton
	public static class ReadonlyDocCache {
		public static DomDocument.ReadonlyDocCache get() {
			return Registry.impl(DomDocument.ReadonlyDocCache.class);
		}

		private int maxSize = 0;

		private Map<String, DomDocument> docs = new LinkedHashMap<String, DomDocument>() {
			@Override
			protected boolean
					removeEldestEntry(Map.Entry<String, DomDocument> eldest) {
				return size() > maxSize;
			}
		};

		int missCount = 0;

		int hitCount = 0;

		public synchronized DomDocument get(String xml) {
			DomDocument doc = docs.get(xml);
			if (doc == null) {
				doc = new DomDocument(xml);
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
