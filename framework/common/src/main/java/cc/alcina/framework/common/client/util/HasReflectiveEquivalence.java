package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;

public interface HasReflectiveEquivalence<T> extends HasEquivalence<T> {
	default boolean debugInequivalence(Property property, Object o1,
			Object o2) {
		if (LooseContext.is(
				HasEquivalence.HasEquivalenceHelper.CONTEXT_IGNORE_FOR_DEBUGGING)) {
			return false;
		} else {
			return false;
		}
	}

	@Override
	default int equivalenceHash() {
		int hash = 0;
		List<Property> properties = Reflections.at(getClass()).properties();
		for (Property property : properties) {
			if (property.isReadOnly() || property.isWriteOnly()
					|| property.has(HasReflectiveEquivalence.Ignore.class)) {
				continue;
			}
			Object o1 = property.get(this);
			hash ^= HasEquivalenceHelper.hash(o1);
		}
		return hash;
	}

	@Override
	default boolean equivalentTo(T other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		List<Property> properties = Reflections.at(getClass()).properties();
		for (Property property : properties) {
			if (property.isReadOnly() || property.isWriteOnly()
					|| property.has(HasReflectiveEquivalence.Ignore.class)) {
				continue;
			}
			Object o1 = property.get(this);
			Object o2 = property.get(other);
			if (CommonUtils.equalsWithNullEquality(o1, o2)) {
				continue;
			} else {
				if (o1 == null || o2 == null) {
					return debugInequivalence(property, o1, o2);
				}
				boolean bothCollections = o1 instanceof Collection
						&& o2 instanceof Collection;
				if (o1.getClass() != o2.getClass() && !bothCollections) {
					return debugInequivalence(property, o1, o2);
				} else if (o1 instanceof HasEquivalence) {
					if (((HasEquivalence) o1)
							.equivalentTo((HasEquivalence) o2)) {
						continue;
					} else {
						return debugInequivalence(property, o1, o2);
					}
				} else if (bothCollections) {
					Collection c1 = (Collection) o1;
					Collection c2 = (Collection) o2;
					if (c1.size() == c2.size()
							&& (c1.iterator().next() instanceof HasEquivalence)
							&& HasEquivalenceHelper.equivalent(c1, c2)) {
						continue;
					} else {
						if (c1.size() == c2.size()) {
							HasEquivalenceHelper.equivalent(c1, c2);
						}
						return debugInequivalence(property, o1, o2);
					}
				} else {
					return debugInequivalence(property, o1, o2);
				}
			}
		}
		return true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.METHOD })
	@ClientVisible
	public @interface Ignore {
	}
}