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

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.csobjects.KnownNodeMetadata.KnownNodeProperty;
import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

@Bean
public class KnownRenderableNode implements Serializable {
	private List<KnownRenderableNode> children = new ArrayList<>();

	private String name;

	private List<KnownTag> tags = new ArrayList<>();

	private String value;

	private Date dateValue;

	private OpStatus opStatusValue;

	private String message;

	private String customiserClassName;

	private List<NamedParameter> customiserParameters = new ArrayList<>();

	private KnownRenderableNode parent;

	private boolean property;

	private transient Object typedValue;

	private transient Object field;

	private KnownNodeMetadata nodeMetadata;

	private KnownNodeProperty propertyMetadata;

	public KnownRenderableNode() {
	}

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

	public int depth() {
		return parent == null ? 0 : parent.depth() + 1;
	}

	public List<KnownRenderableNode> getChildren() {
		return children;
	}

	public String getCustomiserClassName() {
		return customiserClassName;
	}

	public List<NamedParameter> getCustomiserParameters() {
		return customiserParameters;
	}

	public Date getDateValue() {
		return dateValue;
	}

	@AlcinaTransient
	@JsonIgnore
	public Object getField() {
		return field;
	}

	public String getMessage() {
		return message;
	}

	public String getName() {
		return name;
	}

	public KnownNodeMetadata getNodeMetadata() {
		return nodeMetadata;
	}

	public OpStatus getOpStatusValue() {
		return opStatusValue;
	}

	public KnownRenderableNode getParent() {
		return parent;
	}

	public KnownNodeProperty getPropertyMetadata() {
		return propertyMetadata;
	}

	public List<KnownTag> getTags() {
		return tags;
	}

	@AlcinaTransient
	@JsonIgnore
	public Object getTypedValue() {
		return typedValue;
	}

	public String getValue() {
		return value;
	}

	public boolean hasProperties() {
		return children.stream().anyMatch(child -> child.property);
	}

	public boolean hasProperty(String childName) {
		return namedChild(childName).isPresent();
	}

	public boolean isProperty() {
		return property;
	}

	public void merge(KnownsDelta clusterDelta) {
		if (clusterDelta.getAdded().size() == 1) {
			children.addAll(clusterDelta.getAdded().get(0).children);
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

	public void setChildren(List<KnownRenderableNode> children) {
		this.children = children;
	}

	public void setCustomiserClassName(String customiserClassName) {
		this.customiserClassName = customiserClassName;
	}

	public void
			setCustomiserParameters(List<NamedParameter> customiserParameters) {
		this.customiserParameters = customiserParameters;
	}

	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	public void setField(Object field) {
		this.field = field;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNodeMetadata(KnownNodeMetadata nodeMetadata) {
		this.nodeMetadata = nodeMetadata;
	}

	public void setOpStatusValue(OpStatus opStatusValue) {
		this.opStatusValue = opStatusValue;
	}

	public void setParent(KnownRenderableNode parent) {
		this.parent = parent;
	}

	public void setProperty(boolean property) {
		this.property = property;
	}

	public void setPropertyMetadata(KnownNodeProperty propertyMetadata) {
		this.propertyMetadata = propertyMetadata;
	}

	public void setTags(List<KnownTag> tags) {
		this.tags = tags;
	}

	public void setTypedValue(Object typedValue) {
		this.typedValue = typedValue;
	}

	public void setValue(String value) {
		this.value = value;
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

	private Optional<KnownRenderableNode> namedChild(String childName) {
		return children.stream().filter(child -> child.name.equals(childName))
				.findFirst();
	}
}
