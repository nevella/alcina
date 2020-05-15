package cc.alcina.framework.common.client.dom;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;

public class DomNodePropertyAccessor implements PropertyAccessor {
	private List<String> singleChildElementNames = new ArrayList<>();

	public DomNodePropertyAccessor() {
	}

	public DomNodePropertyAccessor(String... singleChildElementNames) {
		this.singleChildElementNames = Arrays.asList(singleChildElementNames);
	}

	public <T> T get(Object bean, String propertyName) {
		return (T) getPropertyValue(bean, propertyName);
	}

	@Override
	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public PropertyReflector getPropertyReflector(Class clazz,
			String propertyName) {
		return Reflections.propertyAccessor().getPropertyReflector(clazz,
				propertyName);
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		throw new UnsupportedOperationException();
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
					.map(v -> node.doc.nodeFor(node.domDoc().adoptNode(v.node)))
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
