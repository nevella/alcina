package cc.alcina.framework.entity.parser.structured;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	public void propertyDelta(String key, boolean add) {
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

	private NodeAncestors outAncestors() {
		return new NodeAncestors(out.getOutCursor(), NodeAncestorsTypes.TARGET);
	}

	protected void closeOpenOutputWrappers(XmlTokenNode node) {
		for (Iterator<XmlTokenNode> itr = openNodes.iterator(); itr
				.hasNext();) {
			XmlTokenNode openNode = itr.next();
			if (!openNode.sourceNode.isAncestorOf(node.sourceNode)
					&& openNode.targetNode != null
					&& openNode.targetNode.tagIs(openNode.token
							.getOutputContext(openNode).getTag())) {
				out.close(openNode,
						openNode.token.getOutputContext(openNode).getTag());
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

		public NodeAncestorIterator(XmlTokenNode tokenNode, boolean output) {
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
			return result;
		}
	}

	interface NodeAncestorsContextProvider {
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
			return new NodeAncestorsContext(this);
		}

		public Iterable<XmlTokenNode> nodeIterable() {
			return () -> nodeIterator();
		}

		public Iterator<XmlTokenNode> nodeIterator() {
			return new NodeAncestorIterator(node,
					types.contains(NodeAncestorsTypes.TARGET));
		}

		public Stream<XmlTokenNode> nodeStream() {
			return StreamSupport.stream(nodeIterable().spliterator(), false);
		}
	}

	public class NodeAncestorsContext
			implements Iterator<XmlTokenOutputContext> {
		@SuppressWarnings("unused")
		private NodeAncestors nodeAncestors;

		private Iterator<XmlTokenNode> itr;

		public NodeAncestorsContext(NodeAncestors nodeAncestors) {
			this.nodeAncestors = nodeAncestors;
			itr = nodeAncestors.nodeIterator();
		}

		@Override
		public boolean hasNext() {
			return itr.hasNext();
		}

		@Override
		public XmlTokenOutputContext next() {
			XmlTokenNode node = itr.next();
			return node.token.getOutputContext(node);
		}
	}

	public enum NodeAncestorsTypes {
		SOURCE, TARGET, NODE
	}
	public void handleOutOfOrderNode(XmlNode node) {
		parser.handleNode(node, this);
		stream.skip(node);
	}
}
