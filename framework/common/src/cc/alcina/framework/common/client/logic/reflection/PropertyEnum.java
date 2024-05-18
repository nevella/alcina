package cc.alcina.framework.common.client.logic.reflection;

import java.util.Objects;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

/**
 * Note - an implementing type need not necessarily be an enum
 */
public interface PropertyEnum {
	public static String asPropertyName(Object name) {
		if (name == null) {
			return null;
		} else if (name instanceof String) {
			return (String) name;
		} else if (name instanceof PropertyEnum) {
			return ((PropertyEnum) name).name();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	String name();

	default Predicate<? super DomainTransformEvent> transformFilter() {
		return event -> Objects.equals(event.getPropertyName(), name());
	}
}