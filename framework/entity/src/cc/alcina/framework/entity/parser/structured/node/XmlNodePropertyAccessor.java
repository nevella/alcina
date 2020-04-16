package cc.alcina.framework.entity.parser.structured.node;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
import cc.alcina.framework.entity.domaintransform.MethodIndividualPropertyAccessor;

public class XmlNodePropertyAccessor implements PropertyAccessor {
	private List<String> singleChildElementNames = new ArrayList<>();

	public XmlNodePropertyAccessor() {
	}

	public XmlNodePropertyAccessor(String... singleChildElementNames) {
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
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	@Override
	public Class getPropertyType(Class clazz, String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getPropertyValue(Object bean, String propertyName) {
		XmlNode node = (XmlNode) bean;
		List<XmlNode> resolved = node.xpath(propertyName).nodes();
		if (resolved.size() == 0) {
			return null;
		}
		if (singleChildElementNames.contains(propertyName)) {
			Preconditions.checkArgument(resolved.size() == 1);
			return resolved.get(0);
		}
		Preconditions.checkArgument(resolved.size() == 1);
		XmlNode singleResolved = resolved.get(0);
		List<XmlNode> elements = singleResolved.children.elements();
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
		XmlNode node = (XmlNode) bean;
		XmlNode leaf = node.ensurePath(propertyName);
		if (value instanceof Collection) {
			Collection<XmlNode> values = (Collection<XmlNode>) value;
			values = values.stream()
					.map(v -> node.doc.nodeFor(node.domDoc().adoptNode(v.node)))
					.collect(Collectors.toList());
			leaf.children.append(values);
		} else {
			if (singleChildElementNames.contains(propertyName)) {
				XmlNode nodeValue = (XmlNode) value;
				if (nodeValue.children.nodes().size() == 0) {
					leaf.removeFromParent();
				} else {
					XmlNode imported = leaf.children.importFrom(nodeValue);
					leaf.strip();
				}
			} else {
				leaf.setText(value.toString());
			}
		}
	}
}
