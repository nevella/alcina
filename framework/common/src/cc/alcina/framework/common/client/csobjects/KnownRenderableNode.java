package cc.alcina.framework.common.client.csobjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata.KnownNodeProperty;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class KnownRenderableNode implements Serializable {
	public List<KnownRenderableNode> children = new ArrayList<>();

	public String name;

	public List<KnownTag> tags = new ArrayList<>();

	public String value;

	public Date dateValue;

	public OpStatus opStatusValue;

	public String message;

	public String customiserClassName;

	public List<NamedParameter> customiserParameters = new ArrayList<>();

	public KnownRenderableNode parent;

	public boolean property;

	public transient Object typedValue;

	public transient Object field;

	public KnownNodeMetadata nodeMetadata;

	public KnownNodeProperty propertyMetadata;

	public List<KnownRenderableNode> allNodes() {
		Stack<KnownRenderableNode> stack = new Stack<KnownRenderableNode>();
		stack.push(this);
		List<KnownRenderableNode> nodes = new ArrayList<>();
		while (!stack.isEmpty()) {
			KnownRenderableNode node = stack.pop();
			nodes.add(node);
			node.children.stream().forEach(stack::add);
		}
		return nodes;
	}
	public KnownRenderableNode() {
	}

	public KnownRenderableNode byPath(String path) {
		return byPath(path, true);
	}

	public KnownTag calculateStatus() {
		if (status() != null) {
			return status();
		}
		return children.stream().map(KnownRenderableNode::status)
				.filter(Objects::nonNull).distinct().sorted()
				.reduce((first, second) -> second).orElse(null);
	}

	public boolean hasProperties() {
		return children.stream().anyMatch(child -> child.property);
	}

	public void merge(KnownsDelta clusterDelta) {
		if (clusterDelta.added.size() == 1) {
			children.addAll(clusterDelta.added.get(0).children);
		}
	}

	public String path() {
		KnownRenderableNode cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			if (cursor.name != null) {
				segments.add(cursor.name);
			}
			cursor = cursor.parent;
		}
		Collections.reverse(segments);
		return segments.stream().collect(Collectors.joining("."));
	}

	public void removeFromParent() {
		parent.children.remove(this);
		parent = null;
	}

	public KnownTagAlcina status() {
		return (KnownTagAlcina) tags.stream()
				.filter(tag -> tag.parent() == KnownTagAlcina.Status)
				.findFirst().orElse(null);
	}

	@Override
	public String toString() {
		return Ax.format("%s:%s", name,
				property ? CommonUtils.trimToWsChars(value, 20)
						: Ax.format("[%s]", children.size()));
	}

	private KnownRenderableNode byPath(String path, boolean first) {
		if (path == null) {
			return name == null ? this : null;
		}
		String segment = path;
		String remainder = null;
		int idx = path.indexOf(".");
		if (idx != -1) {
			segment = path.substring(0, idx);
			remainder = path.substring(idx + 1);
		}
		if (segment.equals(name) && first) {
			return byPath(remainder, false);
		}
		String f_segment = segment;
		Optional<KnownRenderableNode> o_child = children.stream()
				.filter(child -> child.name.equals(f_segment)).findFirst();
		if (o_child.isPresent()) {
			KnownRenderableNode child = o_child.get();
			if (remainder == null) {
				return child;
			} else {
				return child.byPath(remainder, false);
			}
		} else {
			return null;
		}
	}

	public <T> T typedChildValue(Class<T> clazz, String childName) {
		KnownRenderableNode namedChild = namedChild(childName).get();
		String childValue = namedChild.value;
		if (childValue == null) {
			return null;
		}
		if (clazz == Date.class) {
			return (T) namedChild.dateValue;
		}
		if (clazz == OpStatus.class) {
			return (T) namedChild.opStatusValue;
		}
		return (T) childValue;
	}

	public boolean hasProperty(String childName) {
		return namedChild(childName).isPresent();
	}

	private Optional<KnownRenderableNode> namedChild(String childName) {
		return children.stream().filter(child -> child.name.equals(childName))
				.findFirst();
	}
}
