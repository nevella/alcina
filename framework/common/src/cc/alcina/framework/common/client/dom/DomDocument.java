package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ranges.DocumentRange;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.dom.DomEnvironment.NamespaceResult;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.Topic;

public class DomDocument extends DomNode implements Cloneable {
	// for server-side code to link w3c docs to the DomDocument
	public static Topic<DomDocument> topicDocumentCreated = Topic.create();

	private static volatile transient PerDocumentSupplier perDocumentSupplier;

	public static DomDocument basicHtmlDoc() {
		return DomDocument.from("<html><head></head><body></body></html>");
	}

	public DomDocument clone() {
		DomDocument clone = from(fullToString());
		clone.setReadonly(isReadonly());
		return clone;
	}

	public static DomNode createDocumentElement(String tag) {
		return DomDocument.from(Ax.format("<%s/>", tag))
				.getDocumentElementNode();
	}

	public static DomDocument createTextContainer(String text) {
		DomDocument document = DomDocument.from("<container/>");
		document.getDocumentElementNode().setText(text);
		document.setReadonly(true);
		return document;
	}

	public static DomDocument from(Document document) {
		if (perDocumentSupplier == null) {
			synchronized (DomDocument.class) {
				if (perDocumentSupplier == null) {
					perDocumentSupplier = Registry
							.impl(PerDocumentSupplier.class);
				}
			}
		}
		return perDocumentSupplier.get(document);
	}

	public static DomDocument from(Document document, boolean ignoreCache) {
		return new DomDocument(document);
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

	/*
	 * Normally this will be null, unless the DomDocument has no backing w3c/gwt
	 * document
	 */
	private DomNode documentElementDomNode;

	public void setDocumentElementDomNode(DomNode documentElementDomNode) {
		this.documentElementDomNode = documentElementDomNode;
	}

	private DomDocument(Document w3cDocument) {
		super(null, null);
		initNodes(1000);
		this.node = w3cDocument;
		nodes.put(this.node, this);
		this.document = this;
		topicDocumentCreated.publish(this);
	}

	private DomDocument(String xml) {
		super(null, null);
		loadFromXml(xml);
		topicDocumentCreated.publish(this);
	}

	Multimap<String, List<DomNode>> byId() {
		ensureByLookups();
		return byId;
	}

	Multimap<String, List<DomNode>> byTag() {
		ensureByLookups();
		return byTag;
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

	private void ensureByLookups() {
		if (byTag == null) {
			byTag = new Multimap<>();
			byId = new Multimap<>();
			byTag = getDocumentElementNode().stream()
					.collect(AlcinaCollectors.toKeyMultimap(DomNode::name));
			byId = getDocumentElementNode().stream().filter(n -> n.has("id"))
					.collect(AlcinaCollectors.toKeyMultimap(n -> n.attr("id")));
		}
	}

	public DomNode getDocumentElementNode() {
		return documentElementDomNode != null ? documentElementDomNode
				: nodeFor(domDoc().getDocumentElement());
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

	private void initNodes(int length) {
		nodes = CollectionCreators.Bootstrap.getHashMapCreator()
				.create(length / 20);
	}

	public boolean isReadonly() {
		return this.readonly;
	}

	private void loadFromXml(String xml) {
		initNodes(xml.length());
		try {
			this.node = DomEnvironment.get().loadFromXml(xml, false);
			nodes.put(this.node, this);
			this.document = this;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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

	public void normaliseWhitespace() {
		stream().filter(DomNode::isText).forEach(n -> {
			String textContent = n.textContent();
			String normalized = TextUtils.normalizeWhitespace(textContent);
			if (!Objects.equals(textContent, normalized)) {
				n.setText(normalized);
			}
		});
	}

	@Override
	public String prettyToString() {
		try {
			return DomEnvironment.get().prettyPrint(domDoc());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	void register(DomNode xmlNode) {
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

	// FIXME - remove with universal mutable location support
	public void invalidateLocations() {
		if (locations != null) {
			locations.invalidateLookups();
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
	public class Locations implements LocationContext {
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

		Location asLocation(DomNode domNode) {
			ensureLookups();
			Location location = byNode.get(domNode);
			if (location == null) {
				// FIXME - measure
				invalidateLookups();
				ensureLookups();
				location = byNode.get(domNode);
				Preconditions.checkNotNull(location);
			}
			return location;
		}

		Location.Range asRange(DomNode domNode) {
			Location start = asLocation(domNode);
			Location end = null;
			if (domNode.isText()) {
				end = createTextRelativeLocation(start,
						contentLengths.get(domNode), true);
			} else {
				end = asLocation(domNode).clone();
				end.index += contentLengths.get(domNode);
				// only for non-text (text locations do not use after)
				end.after = true;
			}
			return new Location.Range(start, end);
		}

		private void attachMutationListener() {
			MutableDocument mutableDocument = Registry
					.impl(MutableDocumentDecorator.class)
					.asMutableDocument(document);
			mutableDocument.topicMutationOccurred()
					.add(() -> invalidateLookups());
		}

		@Override
		public Location createTextRelativeLocation(Location location,
				int offset, boolean after) {
			ensureLookups();
			int index = location.index + offset;
			/*
			 * Special case, preserve existing node if possible)
			 */
			DomNode containingNode = location.containingNode;
			int contentLength = contentLengths.get(containingNode);
			int relativeIndex = location.index + offset
					- byNode.get(containingNode).index;
			if (relativeIndex >= 0 && relativeIndex <= contentLength) {
				if (relativeIndex == contentLength && contentLength != 0) {
					after = true;
				}
				return new Location(location.treeIndex, index, after,
						location.containingNode, this);
			}
			Location test = new Location(-1, index, after, null, this);
			Location containingLocation = getContainingLocation(test);
			return new Location(containingLocation.treeIndex, index,
					location.after, containingLocation.containingNode, this);
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

		Location getContainingLocation(Location test) {
			ensureLookups();
			if (test.treeIndex != -1) {
				DomNode node = byTreeIndex.get(test.treeIndex);
				return byNode.get(node);
			}
			// searches for the lowest text node containing location
			int treeIndex = Arrays.binarySearch(locations, test,
					new IndexOnlyComparator());
			if (treeIndex < 0) {
				// search from -treeIndex (which will be minimal node with
				// index>text.index) backwards)
				Location lastTextNode = null;
				treeIndex = -treeIndex - 1;
				if (treeIndex == locations.length) {
					treeIndex--;
				}
				for (; treeIndex >= 0; treeIndex--) {
					Location location = locations[treeIndex];
					if (location.containingNode.isText()
							&& location.index <= test.index) {
						lastTextNode = location;
						break;
					}
				}
				if (lastTextNode != null) {
					String content = lastTextNode.containingNode.textContent();
					Preconditions.checkState(lastTextNode.index
							+ content.length() >= test.index);
				}
				if (treeIndex == -1) {
					throw new UnsupportedOperationException();
				}
			}
			int cursor = treeIndex;
			if (test.after) {
				// there's no non-empty text node ending at index 0
				Preconditions.checkArgument(test.index != 0);
				while (cursor >= 0) {
					Location found = locations[cursor];
					// will loop until found.index < test.index (which will
					// be the text node that ends at test.index)
					if (found.index < test.index) {
						treeIndex = cursor;
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
						treeIndex = cursor - 1;
						break;
					} else {
						cursor++;
					}
				}
				// edge case, last text node
				if (treeIndex == -1) {
					treeIndex = locations.length - 1;
				}
			}
			return locations[treeIndex];
		}

		@Override
		public DomNode getContainingNode(Location test) {
			return getContainingLocation(test).containingNode;
		}

		/*
		 * This is fairly optimal, O(log(n))
		 */
		@Override
		public List<DomNode> getContainingNodes(int index, boolean after) {
			ensureLookups();
			List<DomNode> result = new ArrayList<>();
			Location start = byNode.get(getDocumentElementNode());
			while (!start.isTextNode()) {
				start = start.relativeLocation(RelativeDirection.NEXT_LOCATION);
			}
			Location test = start.createTextRelativeLocation(index, after);
			Location containingLocation = test;
			while (!containingLocation.isTextNode()) {
				containingLocation = containingLocation
						.relativeLocation(RelativeDirection.NEXT_LOCATION);
			}
			Preconditions.checkState(containingLocation.index >= index
					&& containingLocation.index <= index
							+ containingLocation.containingNode.textContent()
									.length());
			DomNode cursor = containingLocation.containingNode;
			do {
				result.add(cursor);
				cursor = cursor.parent();
			} while (cursor.isElement());
			Collections.reverse(result);
			return result;
		}

		Location.Range getDocumentRange() {
			ensureLookups();
			DomNode documentElementNode = getDocumentElementNode();
			Location start = byNode.get(documentElementNode);
			Location end = new Location(0, contents.length(), true,
					documentElementNode, this);
			return new Location.Range(start, end);
		}

		/**
		 * <p>
		 * Implementation for text is a little complicated, because "next"
		 * depends on the caller to a degree
		 * 
		 * <p>
		 * Note - don't use this for purely text traversal (e.g. mimicking
		 * actions of a keyevent), instead just increment/decrement
		 * location.index, and use
		 * {@link #createTextRelativeLocation(Location, int, boolean)}
		 */
		@Override
		public Location getRelativeLocation(Location location,
				RelativeDirection direction, TextTraversal textTraversal) {
			/*
			 * See Location for a visual explanation of traversal
			 */
			DomNode node = location.containingNode;
			int targetTreeIndex = location.treeIndex;
			int targetIndex = location.index;
			boolean targetAfter = !location.after;
			Location baseLocation = byNode.get(node);
			Location parentLocation = byNode.get(node.parent());
			boolean nodeTraversalRequired = false;
			if (direction == RelativeDirection.CURRENT_NODE_END) {
				Preconditions.checkArgument(targetAfter);
				Integer length = contentLengths.get(node);
				targetIndex = baseLocation.index + length;
				return new Location(targetTreeIndex, targetIndex, targetAfter,
						node, this);
			}
			if (node.isText()) {
				int relativeIndex = location.index - baseLocation.index;
				switch (direction) {
				case NEXT_LOCATION: {
					if (relativeIndex == node.textContent().length()) {
						nodeTraversalRequired = true;
					} else {
						switch (textTraversal) {
						case NEXT_CHARACTER:
							targetIndex++;
							break;
						case NO_CHANGE:
							break;
						case EXIT_NODE:
							nodeTraversalRequired = true;
							break;
						case TO_END_OF_NODE:
							targetIndex = baseLocation.index
									+ node.textContent().length();
							break;
						default:
							throw new UnsupportedOperationException();
						}
					}
					break;
				}
				case PREVIOUS_LOCATION: {
					// reversed traversal does not currently match partial nodes
					nodeTraversalRequired = true;
					break;
				}
				case PREVIOUS_DOMNODE_START: {
					nodeTraversalRequired = true;
					break;
				}
				case NEXT_DOMNODE_START: {
					nodeTraversalRequired = true;
					break;
				}
				default:
					throw new UnsupportedOperationException();
				}
			} else {
				nodeTraversalRequired = true;
			}
			if (nodeTraversalRequired) {
				targetIndex = -1;
				switch (direction) {
				case NEXT_LOCATION: {
					if (location.after) {
						DomNode nextSibling = node.relative().nextSibling();
						if (nextSibling == null) {
							// last, ascend
							targetTreeIndex = parentLocation != null
									? parentLocation.treeIndex
									: 0;
							targetAfter = true;
						} else {
							targetTreeIndex = byNode.get(nextSibling).treeIndex;
						}
					} else {
						// descend or go to next sibling
						DomNode next = node.children.firstNode();
						next = next != null ? next
								: node.relative().nextLogicalNode();
						if (next == null) {
							// top, ascend
							targetTreeIndex = parentLocation != null
									? parentLocation.treeIndex
									: 0;
						} else {
							targetTreeIndex = byNode.get(next).treeIndex;
							targetAfter = false;
						}
					}
					break;
				}
				case PREVIOUS_LOCATION: {
					if (!location.after) {
						DomNode previousSibling = node.relative()
								.previousSibling();
						if (previousSibling == null) {
							// last, ascend
							targetTreeIndex = parentLocation != null
									? parentLocation.treeIndex
									: 0;
							targetAfter = false;
						} else {
							targetTreeIndex = byNode
									.get(previousSibling).treeIndex;
						}
					} else {
						DomNode lastChild = node.children.lastNode();
						if (lastChild == null) {
							// just the start of the current node
						} else {
							// end of the last child
							targetTreeIndex = byNode.get(lastChild).treeIndex;
							targetAfter = true;
						}
					}
					break;
				}
				case PREVIOUS_DOMNODE_START: {
					// if at start, go to previous logical node - if at end, go
					// to last descendant
					targetAfter = false;
					if (!location.after) {
						targetTreeIndex--;
					} else {
						DomNode lastDescendant = node.relative()
								.lastDescendant();
						if (lastDescendant != null) {
							targetTreeIndex = byNode
									.get(lastDescendant).treeIndex;
						} else {
							targetTreeIndex--;
						}
					}
					break;
				}
				case NEXT_DOMNODE_START: {
					// if at start, go to next logical node from start - if at
					// end, go
					// next logical node from last descendant
					targetAfter = false;
					if (!location.after) {
						targetTreeIndex++;
					} else {
						DomNode lastDescendant = node.relative()
								.lastDescendant();
						if (lastDescendant != null) {
							targetTreeIndex = byNode
									.get(lastDescendant).treeIndex;
							targetTreeIndex++;
						} else {
							targetTreeIndex++;
						}
					}
					break;
				}
				default:
					throw new UnsupportedOperationException();
				}
			}
			DomNode containingNode = byTreeIndex.get(targetTreeIndex);
			if (targetIndex == -1) {
				Location nodeLocation = containingNode.asLocation();
				targetIndex = nodeLocation.index;
				if (targetAfter) {
					if (containingNode.isText()
							&& textTraversal == TextTraversal.TO_START_OF_NODE) {
					} else {
						targetIndex += contentLengths.get(containingNode);
					}
				}
			}
			return new Location(targetTreeIndex, targetIndex, targetAfter,
					containingNode, this);
		}

		@Override
		public String getSubsequentText(Location location, int chars) {
			return contents.substring(location.index,
					Math.min(contents.length(), location.index + chars));
		}

		private void invalidateLookups() {
			byNode = null;
		}

		@Override
		public String markupContent(Range range) {
			DomNode node = range.containingNode();
			if (node.isText()) {
				return range.text();
			}
			if (range.start.containingNode == node
					&& range.end.containingNode == node) {
				String markup = node.fullToString();
				// if namespaced, return full
				// FIXME - selection - have a 'robust pretty' that uses a
				// variant on Element.getOuterHtml()
				if (markup.matches(
						"(?s).*(</[a-zA-Z9-9]+:[a-zA-Z9-9]+>|&nbsp;).*")) {
					return markup;
				} else {
					return node.prettyToString();
				}
			} else {
				if (!(domDoc() instanceof DocumentRange)) {
					Ax.sysLogHigh(
							"truncating markup - DocumentRange not implemented for gwt docs");
					return range.start.containingNode.fullToString();
				} else {
					org.w3c.dom.ranges.Range w3cRange = ((DocumentRange) domDoc())
							.createRange();
					if (range.start.containingNode.isText()) {
						w3cRange.setStart(range.start.containingNode.node,
								range.start.indexInNode());
					} else {
						w3cRange.setStartBefore(
								range.start.containingNode.node);
					}
					if (range.end.containingNode.isText()) {
						w3cRange.setEnd(range.end.containingNode.node,
								range.end.indexInNode());
					} else {
						w3cRange.setEndAfter(range.end.containingNode.node);
					}
					DocumentFragment fragment = w3cRange.cloneContents();
					Element fragmentContainer = domDoc()
							.createElement("fragment-container");
					fragmentContainer.appendChild(fragment);
					return DomNode.from(fragmentContainer).fullToString();
				}
			}
		}

		@Override
		public String textContent(Location.Range range) {
			ensureLookups();
			return contents.substring(range.start.index, range.end.index);
		}

		@Override
		public int toValidIndex(int idx) {
			ensureByLookups();
			if (idx < 0) {
				return 0;
			}
			if (idx > contents.length()) {
				idx = contents.length();
			}
			return idx;
		}

		class IndexOnlyComparator implements Comparator<Location> {
			@Override
			public int compare(Location l1, Location l2) {
				return compareIndexOnly(l1, l2);
			}
		}

		@Override
		public void invalidate() {
			invalidateLookups();
		}
	}

	public interface MutableDocument {
		Topic<Void> topicMutationOccurred();
	}

	public interface MutableDocumentDecorator {
		MutableDocument asMutableDocument(DomDocument document);
	}

	/*
	 * GWT Document instances retain a reference to the DomDocument
	 */
	public interface PerDocumentSupplier {
		public DomDocument get(Document document);
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
				doc = DomDocument.from(xml);
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
	 * This is either passthrough or effectively a rewrite of a xerces (possibly
	 * HTML) doc as a GWT/XML doc, for consistent markup writing etc
	 */
	public static DomDocument toGwtDocument(DomDocument nonGwtDoc) {
		try {
			Document gwtDoc = (Document) DomEnvironment.get()
					.loadFromXml("<d/>", true);
			Node intermediateNode = gwtDoc.importNode(
					nonGwtDoc.getDocumentElementNode().w3cNode(), true);
			// rewrite again -
			Document secondPhaseDoc = (Document) DomEnvironment.get()
					.loadFromXml(DomNode.from(intermediateNode).fullToString(),
							true);
			return DomDocument.from(secondPhaseDoc);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	/**
	 * Cleanup things like DocumentFragment trees from #nodes
	 */
	public void removeDetachedNodeReferences() {
		/*
		 * find detach roots
		 */
		List<Node> detachRoots = nodes.keySet().stream().filter(
				n -> n.getParentNode() == null && n.getOwnerDocument() != null)
				.toList();
		Set<Node> toDetach = new HashSet<>();
		detachRoots.forEach(root -> {
			TreeWalker walker = ((DocumentTraversal) root.getOwnerDocument())
					.createTreeWalker(root, NodeFilter.SHOW_ALL, null, true);
			do {
				toDetach.add(walker.getCurrentNode());
			} while (walker.nextNode() != null);
		});
		nodes.keySet().removeIf(toDetach::contains);
	}
}
