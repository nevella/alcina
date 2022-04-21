package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.collections.PathAccessor;

public class DomNodePathAccessor implements PathAccessor {
	private List<String> singleChildElementNames = new ArrayList<>();

	public DomNodePathAccessor() {
	}

	public DomNodePathAccessor(String... singleChildElementNames) {
		this.singleChildElementNames = Arrays.asList(singleChildElementNames);
	}

	public <T> T get(Object bean, String propertyName) {
		return (T) getPropertyValue(bean, propertyName);
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		DomNode node = (DomNode) bean;
		List<DomNode> resolved = node.xpath(propertyName).nodes();
		if (resolved.size() == 0) {
			return null;
		}
		if (singleChildElementNames.contains(propertyName)) {
			Preconditions.checkArgument(resolved.size() == 1);
			return resolved.get(0);
		}
		Preconditions.checkArgument(resolved.size() == 1);
		DomNode singleResolved = resolved.get(0);
		List<DomNode> elements = singleResolved.children.elements();
		if (elements.size() > 0) {
			return elements;
		} else {
			return singleResolved.textContent();
		}
	}

	@Override
	public boolean hasPropertyKey(Object bean, String propertyName) {
		return getPropertyValue(bean, propertyName) != null;
	}

	@Override
	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		if (value == null) {
			return;
		}
		DomNode node = (DomNode) bean;
		DomNode leaf = node.ensurePath(propertyName);
		if (value instanceof Collection) {
			Collection<DomNode> values = (Collection<DomNode>) value;
			values = values.stream()
					.map(v -> node.document.nodeFor(node.domDoc().adoptNode(v.node)))
					.collect(Collectors.toList());
			leaf.children.append(values);
		} else {
			if (singleChildElementNames.contains(propertyName)) {
				DomNode nodeValue = (DomNode) value;
				if (nodeValue.children.nodes().size() == 0) {
					leaf.removeFromParent();
				} else {
					DomNode imported = leaf.children.importFrom(nodeValue);
					leaf.strip();
				}
			} else {
				leaf.setText(value.toString());
			}
		}
	}
}
