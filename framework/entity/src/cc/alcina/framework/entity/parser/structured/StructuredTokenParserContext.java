package cc.alcina.framework.entity.parser.structured;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.parser.structured.StructuredTokenParserContext.OutputContextRoot;
import cc.alcina.framework.entity.parser.structured.node.XmlNode;
import cc.alcina.framework.entity.parser.structured.node.XmlTokenStream;

public class StructuredTokenParserContext {
	public XmlTokenOutput out;

	public XmlTokenStream stream;

	public Multimap<XmlToken, List<XmlNode>> matched = new Multimap<>();

	private Map<XmlNode, XmlTokenNode> nodeToken = new LinkedHashMap<>();

	StructuredTokenParser parser;

	StringMap properties = new StringMap();

	int lastDepthOut = 0;

	int initialDepthOut = -1;

	int lastDepthIn = 0;

	int initialDepthIn = -1;

	protected LinkedList<XmlTokenNode> openNodes = new LinkedList<>();

	public void end() {
	}

	public boolean had(XmlToken token) {
		return matched.containsKey(token);
	}

	public boolean has(XmlNode node, XmlToken token) {
		node = node.parent();
		while (node != null) {
			XmlTokenNode mappedTo = nodeToken.get(node);
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

	public void log(XmlTokenNode outNode) {
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
		System.out.format("%-30s%-30s%s\n", depthInSpacer + inStr,
				depthOutSpacer + outStr, match);
	}

	public boolean outputOpen(XmlToken token) {
		return outAncestors().nodeStream().anyMatch(xtn -> xtn.token == token);
	}

	public void propertyDelta(XmlTokenNode outNode, String key, boolean add) {
		properties.setBooleanOrRemove(key, add);
	}

	public void skip(XmlNode node) {
		stream.skip(node);
	}

	public void skipChildren() {
		stream.skipChildren();
	}

	public void targetNodeMapped(XmlTokenNode outNode) {
		nodeToken.put(outNode.targetNode, outNode);
	}

	public void wasMatched(XmlTokenNode outNode) {
		matched.add(outNode.token, outNode.sourceNode);
		nodeToken.put(outNode.sourceNode, outNode);
	}

	public NodeAncestorsContextProvider xmlOutputContext() {
		return outAncestors();
	}

	public NodeAncestorsContextProvider xmlInputContext(XmlTokenNode outNode) {
		return new NodeAncestors(outNode, NodeAncestorsTypes.SOURCE);
	}

	protected NodeAncestors outAncestors() {
		return new NodeAncestors(out.getOutCursor(), NodeAncestorsTypes.TARGET);
	}

	public XmlTokenNode getNodeToken(XmlNode node) {
		return nodeToken.get(node);
	}

	protected void closeOpenOutputWrappers(XmlTokenNode node) {
		for (Iterator<XmlTokenNode> itr = openNodes.iterator(); itr
				.hasNext();) {
			XmlTokenNode openNode = itr.next();
			if (!openNode.sourceNode.isAncestorOf(node.sourceNode)
					&& openNode.targetNode != null) {
				out.close(openNode, openNode.targetNode.name());
				itr.remove();
			}
		}
	}

	protected void maybeOpenOutputWrapper(XmlTokenNode node) {
		if (node.token.getOutputContext(node).hasTag()) {
			out.open(node, node.token.getOutputContext(node).getTag(),
					node.token.getOutputContext(node).getEmitAttributes());
			openNodes.push(node);
		}
	}

	public class NodeAncestorIterator implements Iterator<XmlTokenNode> {
		XmlNode cursor;

		private XmlTokenNode tokenNode;

		private boolean root;

		public NodeAncestorIterator(XmlTokenNode tokenNode, boolean output) {
			if (tokenNode == null) {
				return;
			}
			this.tokenNode = tokenNode;
			cursor = output ? tokenNode.targetNode : tokenNode.sourceNode;
		}

		@Override
		public boolean hasNext() {
			return cursor != null && tokenNode != null;
		}

		@Override
		public XmlTokenNode next() {
			XmlTokenNode result = tokenNode;
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

	public interface NodeAncestorsContextProvider {
		public NodeAncestorsContext contexts();
	}

	public class NodeAncestors implements NodeAncestorsContextProvider {
		private EnumSet<NodeAncestorsTypes> types;

		private XmlTokenNode node;

		public NodeAncestors(XmlTokenNode node, NodeAncestorsTypes... types) {
			this.node = node;
			this.types = EnumSet.of(types[0]);
			for (int idx = 1; idx < types.length; idx++) {
				this.types.add(types[idx]);
			}
		}

		public NodeAncestorsContext contexts() {
			return new NodeAncestorsContext(this, isTarget());
		}

		public Iterable<XmlTokenNode> nodeIterable() {
			return () -> nodeIterator();
		}

		public Iterator<XmlTokenNode> nodeIterator() {
			return new NodeAncestorIterator(node, isTarget());
		}

		private boolean isTarget() {
			return types.contains(NodeAncestorsTypes.TARGET);
		}

		public Stream<XmlTokenNode> nodeStream() {
			return StreamSupport.stream(nodeIterable().spliterator(), false);
		}

		public Optional<XmlTokenNode> findOutputNode(String hasProperty) {
			return nodeStream().filter(n -> n.token.getOutputContext(n).is(""))
					.findFirst();
		}

		public XmlTokenNode root() {
			// last
			return node == null ? null
					: nodeStream().reduce((first, second) -> second).get();
		}
	}

	public class NodeAncestorsContext implements Iterator<XmlTokenContext> {
		@SuppressWarnings("unused")
		private NodeAncestors nodeAncestors;

		private Iterator<XmlTokenNode> itr;

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
			XmlTokenNode node = itr.next();
			return target ? node.token.getOutputContext(node)
					: node.token.getInputContext(node);
		}
	}

	public enum NodeAncestorsTypes {
		SOURCE, TARGET, NODE
	}

	public void handleOutOfOrderNode(XmlNode node) {
		parser.handleNode(node, this);
		stream.skip(node);
	}

	public static abstract class OutputContextRoot {
		public XmlTokenNode node;
	}

	Map<XmlTokenNode, OutputContextRoot> outputContextRoots = new LinkedHashMap<>();

	protected <T extends OutputContextRoot> T
			outputContextRoot(XmlTokenNode node, Supplier<T> supplier) {
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
				Optional<XmlTokenNode> rootNodeOptional = outAncestors()
						.findOutputNode(
								XmlTokenContext.P_contextResolutionRoot);
				XmlTokenNode rootNode = rootNodeOptional
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
}
