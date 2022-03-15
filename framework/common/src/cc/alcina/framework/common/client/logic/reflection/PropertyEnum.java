package cc.alcina.framework.common.client.logic.reflection;

import java.util.Objects;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;

public interface PropertyEnum {
	String name();

	default Predicate<? super DomainTransformEvent> transformFilter() {
		return event -> Objects.equals(event.getPropertyName(),
				((Enum) this).name());
	}
}