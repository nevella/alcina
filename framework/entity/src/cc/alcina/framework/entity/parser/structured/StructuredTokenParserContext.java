package cc.alcina.framework.entity.parser.structured;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParserContext {
	public XmlTokenOutput out;

	public XmlTokenStream stream;

	public Multimap<XmlToken, List<XmlNode>> matched = new Multimap<>();

	private Map<XmlNode, XmlStructuralJoin> nodeToken = new LinkedHashMap<>();

	StructuredTokenParser parser;

	StringMap properties = new StringMap();

	int lastDepthOut = 0;

	int initialDepthOut = -1;

	int lastDepthIn = 0;

	int initialDepthIn = -1;

	protected LinkedList<XmlStructuralJoin> openNodes = new LinkedList<>();

	Map<XmlStructuralJoin, OutputContextRoot> outputContextRoots = new LinkedHashMap<>();

	public void end() {
	}

	public String getLogMessage(XmlStructuralJoin outNode) {
		XmlNode targetNode = outNode.targetNode;
		XmlNode sourceNode = outNode.sourceNode;
		int depthOut = lastDepthOut + 1;
		if (targetNode != null) {
			depthOut = targetNode.depth();
			if (initialDepthOut == -1) {
				initialDepthOut = depthOut;
			}
			lastDepthOut = depthOut;
		}
		String depthOutSpacer = CommonUtils.padStringLeft("",
				(depthOut - initialDepthOut) * 2, " ");
		int depthIn = lastDepthIn + 1;
		if (targetNode != null) {
			depthIn = targetNode.depth();
			if (initialDepthIn == -1) {
				initialDepthIn = depthIn;
			}
			lastDepthIn = depthIn;
		}
		String depthInSpacer = CommonUtils.padStringLeft("",
				(depthIn - initialDepthIn) * 2, " ");
		String outStr = targetNode == null ? "(no output)"
				: targetNode.debug().shortRepresentation();
		String inStr = sourceNode == null ? "(no input)"
				: sourceNode.debug().shortRepresentation();
		String match = outNode.token.name;
		String message = String.format("%-30s%-30s%s", depthInSpacer + inStr,
				depthOutSpacer + outStr, match);
		return message;
	}

	public XmlStructuralJoin getNodeToken(XmlNode node) {
		return nodeToken.get(node);
	}

	public boolean had(XmlToken token) {
		return matched.containsKey(token);
	}

	public void handleOutOfOrderNode(XmlNode node) {
		parser.handleNode(node, this);
		stream.skip(node);
	}

	public boolean has(XmlNode node, XmlToken token) {
		node = node.parent();
		while (node != null) {
			XmlStructuralJoin mappedTo = nodeToken.get(node);
			if (mappedTo == null) {
				break;
			}
			if (mappedTo.token == token) {
				return true;
			}
			node = node.parent();
		}
		return false;
	}

	public boolean isOpen(XmlToken token) {
		return openNodes.stream().anyMatch(xtn -> xtn.token == token);
	}

	public boolean isSubCategory(Class clazz) {
		if (out.getOutCursor() == null) {
			return false;
		}
		XmlNode cursor = out.getOutCursor().sourceNode;
		while (cursor != null) {
			if (nodeToken.get(cursor).token.getSubCategory() == clazz) {
				return true;
			}
			cursor = cursor.parent();
		}
		return false;
	}

	public void log(XmlStructuralJoin outNode) {
		System.out.println(getLogMessage(outNode));
	}

	public boolean outputOpen(XmlToken token) {
		return outAncestors(null).nodeStream()
				.anyMatch(xtn -> xtn.token == token);
	}

	public void propertyDelta(XmlStructuralJoin outNode, String key,
			boolean add) {
		properties.setBooleanOrRemove(key, add);
	}

	public void skip(XmlNode node) {
		stream.skip(node);
	}

	public void skipChildren() {
		stream.skipChildren();
	}

	public void skipChildren(Predicate<XmlNode> predicate) {
		stream.skipChildren(predicate);
	}

	public void start() {
	}

	public void targetNodeMapped(XmlStructuralJoin outNode) {
		nodeToken.put(outNode.targetNode, outNode);
	}

	public void wasMatched(XmlStructuralJoin outNode) {
		matched.add(outNode.token, outNode.sourceNode);
		nodeToken.put(outNode.sourceNode, outNode);
	}

	public NodeAncestorsContextProvider xmlInputContext(
			XmlStructuralJoin outNode, Predicate<XmlStructuralJoin> stopNodes) {
		return new NodeAncestors(outNode, stopNodes, NodeAncestorsTypes.SOURCE);
	}

	public NodeAncestorsContextProvider xmlOutputContext() {
		return xmlOutputContext(null);
	}

	protected void closeOpenOutputWrappers(XmlStructuralJoin node) {
		List<String> closed = new ArrayList<>();
		for (Iterator<XmlStructuralJoin> itr = openNodes.iterator(); itr
				.hasNext();) {
			XmlStructuralJoin openNode = itr.next();
			if (!openNode.sourceNode.isAncestorOf(node.sourceNode)
					&& openNode.targetNode != null) {
				String tag = openNode.targetNode.name();
				closed.add(tag);
				out.close(openNode, tag);
				itr.remove();
			}
		}
	}

	protected void maybeOpenOutputWrapper(XmlStructuralJoin node) {
		if (node.token.getOutputContext(node).hasTag()) {
			out.open(node, node.token.getOutputContext(node).getTag(),
					node.token.getOutputContext(node).getEmitAttributes());
			openNodes.push(node);
		}
	}

	protected NodeAncestors outAncestors() {
		return outAncestors(null);
	}

	protected NodeAncestors
			outAncestors(Predicate<XmlStructuralJoin> stopNodes) {
		return new NodeAncestors(out.getOutCursor(), stopNodes,
				NodeAncestorsTypes.TARGET);
	}

	protected <T extends OutputContextRoot> T
			outputContextRoot(XmlStructuralJoin node, Supplier<T> supplier) {
		if (node == null) {
			return (T) outputContextRoots.entrySet().iterator().next()
					.getValue();
		}
		if (!outputContextRoots.containsKey(node)) {
			T root = null;
			if (node.token.getOutputContext(node).isContextResolutionRoot()) {
				root = supplier.get();
				root.node = node;
			} else {
				Optional<XmlStructuralJoin> rootNodeOptional = outAncestors()
						.findOutputNode(
								XmlTokenContext.P_contextResolutionRoot);
				XmlStructuralJoin rootNode = rootNodeOptional
						.orElse(outAncestors().root());
				if (outputContextRoots.containsKey(rootNode)) {
					root = (T) outputContextRoots.get(rootNode);
				} else {
					root = supplier.get();
					root.node = rootNode;
					outputContextRoots.put(rootNode, root);
				}
			}
			outputContextRoots.put(node, root);
		}
		return (T) outputContextRoots.get(node);
	}

	protected NodeAncestorsContextProvider
			xmlOutputContext(Predicate<XmlStructuralJoin> stopNodes) {
		return outAncestors(stopNodes);
	}

	public class NodeAncestorIterator implements Iterator<XmlStructuralJoin> {
		XmlNode cursor;

		private XmlStructuralJoin tokenNode;

		private boolean root;

		private Predicate<XmlStructuralJoin> stopNodes;

		public NodeAncestorIterator(XmlStructuralJoin tokenNode,
				Predicate<XmlStructuralJoin> stopNodes, boolean output) {
			this.stopNodes = stopNodes;
			if (tokenNode == null) {
				return;
			}
			this.tokenNode = tokenNode;
			cursor = output ? tokenNode.targetNode : tokenNode.sourceNode;
		}

		@Override
		public boolean hasNext() {
			return cursor != null && tokenNode != null
					&& (stopNodes == null || stopNodes.test(tokenNode));
		}

		@Override
		public XmlStructuralJoin next() {
			XmlStructuralJoin result = tokenNode;
			cursor = cursor.parent();
			tokenNode = nodeToken.get(cursor);
			if (root) {
				// no next
				tokenNode = null;
			} else {
				if (tokenNode != null) {
					XmlTokenContext outputContext = tokenNode.token
							.getOutputContext(tokenNode);
					if (outputContext != null) {
						root |= outputContext.isContextResolutionRoot();
					}
					root |= tokenNode.token.getInputContext(tokenNode)
							.isContextResolutionRoot();
				}
			}
			if (result == null) {
				int debug = 3;
			}
			return result;
		}
	}

	public class NodeAncestors implements NodeAncestorsContextProvider {
		private EnumSet<NodeAncestorsTypes> types;

		private XmlStructuralJoin node;

		private Predicate<XmlStructuralJoin> stopNodes;

		public NodeAncestors(XmlStructuralJoin node,
				Predicate<XmlStructuralJoin> stopNodes,
				NodeAncestorsTypes... types) {
			this.node = node;
			this.stopNodes = stopNodes;
			this.types = EnumSet.of(types[0]);
			for (int idx = 1; idx < types.length; idx++) {
				this.types.add(types[idx]);
			}
		}

		public NodeAncestorsContext contexts() {
			return new NodeAncestorsContext(this, isTarget());
		}

		public Optional<XmlStructuralJoin> findOutputNode(String hasProperty) {
			return nodeStream().filter(n -> n.token.getOutputContext(n).is(""))
					.findFirst();
		}

		public Iterable<XmlStructuralJoin> nodeIterable() {
			return () -> nodeIterator();
		}

		public Iterator<XmlStructuralJoin> nodeIterator() {
			return new NodeAncestorIterator(node, stopNodes, isTarget());
		}

		public Stream<XmlStructuralJoin> nodeStream() {
			return StreamSupport.stream(nodeIterable().spliterator(), false);
		}

		public XmlStructuralJoin root() {
			// last
			return node == null ? null
					: nodeStream().reduce((first, second) -> second).get();
		}

		private boolean isTarget() {
			return types.contains(NodeAncestorsTypes.TARGET);
		}
	}

	public class NodeAncestorsContext implements Iterator<XmlTokenContext> {
		@SuppressWarnings("unused")
		private NodeAncestors nodeAncestors;

		private Iterator<XmlStructuralJoin> itr;

		private boolean target;

		public NodeAncestorsContext(NodeAncestors nodeAncestors,
				boolean target) {
			this.nodeAncestors = nodeAncestors;
			this.target = target;
			itr = nodeAncestors.nodeIterator();
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public XmlTokenContext next() {
			XmlStructuralJoin node = itr.next();
			return target ? node.token.getOutputContext(node)
					: node.token.getInputContext(node);
		}
	}

	public interface NodeAncestorsContextProvider {
		public NodeAncestorsContext contexts();
	}

	public enum NodeAncestorsTypes {
		SOURCE, TARGET, NODE
	}

	public static abstract class OutputContextRoot {
		public XmlStructuralJoin node;
	}
}
