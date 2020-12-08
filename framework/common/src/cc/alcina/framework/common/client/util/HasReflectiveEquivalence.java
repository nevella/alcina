package cc.alcina.framework.common.client.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;

public interface HasReflectiveEquivalence<T> extends HasEquivalence<T> {
	default boolean debugInequivalence(PropertyInfo pd, Object o1, Object o2) {
		if (LooseContext.is(
				HasEquivalence.HasEquivalenceHelper.CONTEXT_IGNORE_FOR_DEBUGGING)) {
			return false;
		} else {
			return false;
		}
	}

	@Override
	default boolean equivalentTo(T other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		List<PropertyInfo> properties = Reflections.classLookup()
				.getWritableProperties(getClass());
		try {
			for (PropertyInfo pd : properties) {
				Ignore ignore = Reflections.propertyAccessor()
						.getAnnotationForProperty(getClass(),
								HasReflectiveEquivalence.Ignore.class,
								pd.getPropertyName());
				if (ignore != null) {
					continue;
				}
				Object o1 = pd.getReadMethod().invoke(this, new Object[] {});
				Object o2 = pd.getReadMethod().invoke(other, new Object[] {});
				if (CommonUtils.equalsWithNullEquality(o1, o2)) {
					continue;
				} else {
					if (o1 == null || o2 == null) {
						return debugInequivalence(pd, o1, o2);
					}
					boolean bothCollections = o1 instanceof Collection
							&& o2 instanceof Collection;
					if (o1.getClass() != o2.getClass() && !bothCollections) {
						return debugInequivalence(pd, o1, o2);
					} else if (o1 instanceof HasEquivalence) {
						if (((HasEquivalence) o1)
								.equivalentTo((HasEquivalence) o2)) {
							continue;
						} else {
							return debugInequivalence(pd, o1, o2);
						}
					} else if (bothCollections) {
						Collection c1 = (Collection) o1;
						Collection c2 = (Collection) o2;
						if (c1.size() == c2.size()
								&& (c1.iterator()
										.next() instanceof HasEquivalence)
								&& HasEquivalenceHelper.equivalent(c1, c2)) {
							continue;
						} else {
							return debugInequivalence(pd, o1, o2);
						}
					} else {
						return debugInequivalence(pd, o1, o2);
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
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