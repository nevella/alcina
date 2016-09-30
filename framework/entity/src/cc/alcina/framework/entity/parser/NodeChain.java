package cc.alcina.framework.entity.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.parser.NodeChain.NodeChainContext;

public class NodeChain {
	public static NodeChain parentChain(NodeChain.NodeChainContext context,
			Node node) {
		return parentChain(context, node, null, false);
	}

	public static NodeChain parentChain(NodeChain.NodeChainContext context,
			Node from, Node to, boolean excludeEndNodes) {
		NodeChain chain = new NodeChain(context);
		Node node = from;
		chain.fromChild = from;
		while (true) {
			chain.chain.add(0, node);
			if (context.chainEndNodes.contains(node) || to == node) {
				break;
			}
			node = node.getParentNode();
			if (excludeEndNodes) {
				if (context.chainEndNodes.contains(node) || to == node) {
					break;
				}
			}
		}
		return chain;
	}

	Node fromChild;

	public List<Node> chain = new ArrayList<Node>();

	private NodeChain.NodeChainContext context;

	public NodeChain(NodeChain.NodeChainContext context) {
		this.context = context;
	}

	public NodeChain commonElementChain(NodeChain otherChain) {
		NodeChain result = new NodeChain(context);
		for (int idx = 0; idx < chain.size()
				&& idx < otherChain.length(); idx++) {
			if (chain.get(idx) != otherChain.chain.get(idx)) {
				break;
			}
			result.chain.add(chain.get(idx));
		}
		return result;
	}

	public NodeChain
			commonElementChainWithTagEquivalence(NodeChain otherChain) {
		NodeChain result = new NodeChain(context);
		for (int idx = 0; idx < chain.size()
				&& idx < otherChain.length(); idx++) {
			if (!tagNameAt(idx).equals(otherChain.tagNameAt(idx))) {
				break;
			}
			result.chain.add(chain.get(idx));
		}
		return result;
	}

	public boolean contains(String tagName) {
		for (int idx = 0; idx < chain.size(); idx++) {
			if (chain.get(idx).getNodeName().equals(tagName)) {
				return true;
			}
		}
		return false;
	}

	public NodeChain difference(NodeChain otherChain) {
		NodeChain result = new NodeChain(context);
		for (int idx = 0; idx < chain.size(); idx++) {
			if (idx >= otherChain.chain.size()
					|| chain.get(idx) != otherChain.chain.get(idx)) {
				result.chain.add(chain.get(idx));
			}
		}
		return result;
	}

	public boolean equalTagsTo(NodeChain other) {
		if (chain.size() != other.length()) {
			return false;
		}
		for (int idx = 0; idx < chain.size(); idx++) {
			if (!CommonUtils.equalsWithNullEquality(tagNameAt(idx),
					other.tagNameAt(idx))) {
				return false;
			}
		}
		return true;
	}

	public Node get(int idx) {
		return chain.get(idx);
	}

	public boolean isSameBlock(NodeChain otherChain) {
		Node blockAncestor = getBlockAncestor();
		Node otherBlockAncestor = otherChain.getBlockAncestor();
		return blockAncestor == otherBlockAncestor;
	}

	public int length() {
		return chain.size();
	}

	public NodeChain removeFromNode() {
		chain.remove(fromChild);
		return this;
	}

	@Override
	public String toString() {
		return String.format("NodeChain:\n%s", chain.stream().map(n -> {
			if (n == null) {
				return null;
			}
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) n;
				if (e.hasAttribute("class")) {
					return String.format("%s.%s", e.getTagName(),
							e.getAttribute("class"));
				}
			}
			return n.toString();
		}).collect(Collectors.joining("\n")));
	}

	private Node getBlockAncestor() {
		return null;// XmlUtils.getContainingBlock(fromChild);
	}

	private String tagNameAt(int idx) {
		Node n = chain.get(idx);
		if (n.getNodeType() == Node.ELEMENT_NODE) {
			return ((Element) n).getTagName();
		}
		return null;
	}

	public static SplitResult splitAlong(NodeChainContext context,
			NodeChain chain, boolean after) {
		SplitResult result = new SplitResult();
		result.splitIncluding = new NodeChain(context);
		result.splitNotIncluding = new NodeChain(context);
		for (int idx = chain.length() - 1; idx >= 0; idx--) {
			Node current = chain.get(idx);
			result.splitIncluding.chain.add(0, current);
			Node parent = current.getParentNode();
			List<Node> kids = XmlUtils.nodeListToList(parent.getChildNodes());
			int kidIndex = kids.indexOf(current);
			boolean atEdgeClosestToDirection = after
					? kidIndex == kids.size() - 1 : kidIndex == 0;
			boolean cloneParent = result.splitNotIncluding.length() > 0
					|| !atEdgeClosestToDirection;
			if (cloneParent) {
				// splitNotIncluding is always a node ahead of the iteration
				// (we need the parent)
				if (idx > 0) {
					Node clonedParent = parent.cloneNode(false);
					// implies !atEdgeClosestToDirection
					if (after) {
						if (result.splitNotIncluding.length() > 0) {
							clonedParent.appendChild(
									result.splitNotIncluding.get(0));
						}
						for (int idx2 = kidIndex + 1; idx2 < kids
								.size(); idx2++) {
							clonedParent.appendChild(kids.get(idx2));
						}
					} else {
						for (int idx2 = 0; idx2 < kidIndex; idx2++) {
							clonedParent.appendChild(kids.get(idx2));
						}
						if (result.splitNotIncluding.length() > 0) {
							clonedParent.appendChild(
									result.splitNotIncluding.get(0));
						}
					}
					result.splitNotIncluding.chain.add(0, clonedParent);
				}
			}
		}
		if (result.splitNotIncluding.length() > 0) {
			Node incRoot = result.splitIncluding.get(0);
			if (after) {
				incRoot.getParentNode().insertBefore(
						result.splitNotIncluding.get(0),
						incRoot.getNextSibling());
			} else {
				incRoot.getParentNode()
						.insertBefore(result.splitNotIncluding.get(0), incRoot);
			}
		}
		return result;
	}

	public static class NodeChainContext {
		public List<Node> chainEndNodes = new ArrayList<>();
	}

	public static class SplitResult {
		public NodeChain splitIncluding;

		public NodeChain splitNotIncluding;

		boolean notIncludingIsBeforeIncluding;

		void insertChain(NodeChain newChain) {
		}
	}
}