package cc.alcina.framework.entity.parser.structured.node;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.entity.domaintransform.MethodIndividualPropertyAccessor;

public class XmlNodePropertyAccessor implements PropertyAccessor {
	public XmlNodePropertyAccessor() {
	}

	@Override
	public boolean hasPropertyKey(Object bean, String propertyName) {
		return getPropertyValue(bean, propertyName) != null;
	}

	@Override
	public IndividualPropertyAccessor cachedAccessor(Class clazz,
			String propertyName) {
		return new MethodIndividualPropertyAccessor(clazz, propertyName);
	}

	public <T> T get(Object bean, String propertyName) {
		return (T) getPropertyValue(bean, propertyName);
	}

	public <A extends Annotation> A getAnnotationForProperty(Class targetClass,
			Class<A> annotationClass, String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Class getPropertyType(Class clazz, String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Object getPropertyValue(Object bean, String propertyName) {
		XmlNode node = (XmlNode) bean;
		XmlNode resolved = node.xpath(propertyName).node();
		if (resolved == null) {
			return null;
		}
		if (resolved.children.elements().size() > 0) {
			return resolved.children.elements();
		} else {
			return resolved.textContent();
		}
	}

	public void setPropertyValue(Object bean, String propertyName,
			Object value) {
		XmlNode node = (XmlNode) bean;
		XmlNode leaf = node.ensurePath(propertyName);
		if (value instanceof Collection) {
			Collection<XmlNode> values = (Collection<XmlNode>) value;
			values = values.stream()
					.map(v -> node.doc.nodeFor(node.domDoc().adoptNode(v.node)))
					.collect(Collectors.toList());
			leaf.children.append(values);
		} else {
			if (value == null) {
				leaf.removeFromParent();
			} else {
				leaf.setText(value.toString());
			}
		}
	}
}
