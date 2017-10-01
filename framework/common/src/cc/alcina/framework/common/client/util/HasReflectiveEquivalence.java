package cc.alcina.framework.common.client.util;

import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup.PropertyInfoLite;

public interface HasReflectiveEquivalence<T> extends HasEquivalence<T> {
	@Override
	default boolean equivalentTo(T other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		List<PropertyInfoLite> properties = Reflections.classLookup()
				.getWritableProperties(getClass());
		try {
			for (PropertyInfoLite pd : properties) {
				Object o1 = pd.getReadMethod().invoke(this, new Object[] {});
				Object o2 = pd.getReadMethod().invoke(other, new Object[] {});
				if (CommonUtils.equalsWithNullEquality(o1, o2)) {
				} else {
					if (o1 == null || o2 == null) {
						return false;
					}
					if (o1.getClass() != o2.getClass()) {
						return false;
					}
					if (o1 instanceof HasEquivalence) {
						if (((HasEquivalence) o1)
								.equivalentTo((HasEquivalence) o2)) {
						} else {
							return false;
						}
					} else if (o1 instanceof Collection) {
						Collection c1 = (Collection) o1;
						Collection c2 = (Collection) o2;
						if (c1.size() == c2.size()
								&& (c1.iterator()
										.next() instanceof HasEquivalence)
								&& HasEquivalenceHelper.equivalent(c1, c2)) {
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				}
			}
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		return true;
	}
}