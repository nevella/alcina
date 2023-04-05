package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomEnvironment.NamespaceResult;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Topic;

public class DomDocument extends DomNode {
	private static transient PerDocumentSupplier perDocumentSupplier;

	public static DomDocument basicHtmlDoc() {
		return new DomDocument("<html><head></head><body></body></html>");
	}

	public static DomNode createDocumentElement(String tag) {
		return new DomDocument(Ax.format("<%s/>", tag))
				.getDocumentElementNode();
	}

	public static DomDocument createTextContainer(String text) {
		DomDocument document = new DomDocument("<container/>");
		document.getDocumentElementNode().setText(text);
		document.setReadonly(true);
		return document;
	}

	public static DomDocument documentFor(Document document) {
		synchronized (DomDocument.class) {
			if (perDocumentSupplier == null)
				perDocumentSupplier = Registry.impl(PerDocumentSupplier.class);
		}
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

	private Locations locations;

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

	public Location.Range getLocationRange() {
		return locations().getDocumentRange();
	}

	public boolean isReadonly() {
		return this.readonly;
	}

	public Locations locations() {
		if (locations == null) {
			locations = new Locations();
		}
		return locations;
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

	public interface MutableDocument {
		Topic<Void> topicMutationOccurred();
	}

	public interface MutableDocumentDecorator {
		MutableDocument asMutableDocument(DomDocument document);
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

	/*
	 * Mutation support notes:
	 *
	 * Location must reference mutation generation and initially throw if
	 * referencing an older generation
	 *
	 *
	 */
	class Locations implements LocationContext {
		private Map<DomNode, Location> byNode;

		private Map<Integer, DomNode> byTreeIndex;

		private Map<DomNode, Integer> contentLengths;

		private Location[] locations;

		private String contents;

		Locations() {
			// Preconditions.checkState(readonly);
			// FIXME - 1 - add document mutation support
			//
			generateLookups();
			if (!readonly) {
				attachMutationListener();
			}
		}

		@Override
		public Location createRelativeLocation(Location location, int offset,
				boolean after) {
			int index = location.index + offset;
			Location test = new Location(-1, index, after);
			Location containingLocation = getContainingLocation(test);
			return new Location(containingLocation.treeIndex, index,
					location.after, containingLocation.containingNode, this);
		}

		@Override
		public DomNode getContainingNode(Location test) {
			return getContainingLocation(test).containingNode;
		}

		@Override
		public String textContent(Location.Range range) {
			ensureLookups();
			return contents.substring(range.start.index, range.end.index);
		}

		private void attachMutationListener() {
			MutableDocument mutableDocument = Registry
					.impl(MutableDocumentDecorator.class)
					.asMutableDocument(document);
			mutableDocument.topicMutationOccurred()
					.add(() -> invalidateLookups());
		}

		private void ensureLookups() {
			if (byNode == null) {
				generateLookups();
			}
		}

		private void generateLookups() {
			byNode = AlcinaCollections.newLinkedHashMap();
			byTreeIndex = AlcinaCollections.newLinkedHashMap();
			contentLengths = AlcinaCollections.newLinkedHashMap();
			StringBuilder content = new StringBuilder();
			List<DomNode> openNodes = new ArrayList<>();
			DomNode root = getDocumentElementNode();
			// browser dom root has no (Document) parent
			int rootOffset = root.parent() == null ? 0 : 1;
			root.stream().forEach(node -> {
				int depth = node.depth() - rootOffset;
				int treeIndex = byNode.size();
				Location location = new Location(treeIndex, content.length(),
						false, node, this);
				byNode.put(node, location);
				byTreeIndex.put(treeIndex, node);
				contentLengths.put(node, 0);
				if (depth == openNodes.size()) {
					openNodes.add(node);
				} else {
					openNodes.set(depth, node);
				}
				if (node.isText()) {
					content.append(node.textContent());
					int nodeLength = node.textContent().length();
					for (int idx = 0; idx <= depth; idx++) {
						DomNode domNode = openNodes.get(idx);
						contentLengths.put(domNode,
								contentLengths.get(domNode) + nodeLength);
					}
				}
			});
			contents = content.toString();
			locations = (Location[]) new ArrayList<>(byNode.values())
					.toArray(new Location[byNode.size()]);
		}

		private void invalidateLookups() {
			byNode = null;
		}

		Location asLocation(DomNode domNode) {
			ensureLookups();
			return byNode.get(domNode);
		}

		Location.Range asRange(DomNode domNode) {
			Location start = asLocation(domNode);
			Location end = createRelativeLocation(start,
					contentLengths.get(domNode), true);
			end.after = true;
			return new Location.Range(start, end);
		}

		Location getContainingLocation(Location test) {
			ensureLookups();
			if (test.treeIndex != -1) {
				DomNode node = byTreeIndex.get(test.treeIndex);
				return byNode.get(node);
			}
			int index = -1;
			// searches for the lowest text node containing location
			index = Arrays.binarySearch(locations, test,
					new IndexOnlyComparator());
			if (index < 0) {
				// may be contained in the last text node, check
				Location lastTextNode = null;
				int idx = locations.length - 1;
				for (; idx >= 0; idx--) {
					Location location = locations[idx];
					if (location.containingNode.isText()) {
						lastTextNode = location;
						break;
					}
				}
				if (lastTextNode != null) {
					String content = lastTextNode.containingNode.textContent();
					if (lastTextNode.index + content.length() >= test.index) {
						index = idx;
					}
				}
				if (index == -1) {
					throw new UnsupportedOperationException();
				}
			}
			int cursor = index;
			if (test.after) {
				// there's no non-empty text node ending at index 0
				Preconditions.checkArgument(test.index != 0);
				while (cursor >= 0) {
					Location found = locations[cursor];
					// will loop until found.index < test.index (which will
					// be the text node that ends at test.index)
					if (found.index < test.index) {
						index = cursor;
						break;
					} else {
						cursor--;
					}
				}
			} else {
				// there's no non-empty text node starting at index
				// [contents.length]
				Preconditions.checkArgument(test.index != contents.length());
				while (cursor < locations.length) {
					Location found = locations[cursor];
					// will loop until found.index > test.index (which will
					// be the node immediately following the containing text
					// node)
					if (found.index > test.index) {
						index = cursor - 1;
						break;
					} else {
						cursor++;
					}
				}
				// edge case, last text node
				if (index == -1) {
					index = locations.length - 1;
				}
			}
			return locations[index];
		}

		Location.Range getDocumentRange() {
			ensureLookups();
			DomNode documentElementNode = getDocumentElementNode();
			Location start = byNode.get(documentElementNode);
			Location end = new Location(0, contents.length(), true,
					documentElementNode, this);
			return new Location.Range(start, end);
		}

		class IndexOnlyComparator implements Comparator<Location> {
			@Override
			public int compare(Location l1, Location l2) {
				return compareIndexOnly(l1, l2);
			}
		}
	}
}
