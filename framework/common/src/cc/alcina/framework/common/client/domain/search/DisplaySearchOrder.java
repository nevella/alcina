package cc.alcina.framework.common.client.domain.search;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.HasDisplayName;

public class DisplaySearchOrder extends SearchOrder {
	private String fieldName;

	transient BiFunction<Object, Object, Comparable> toComparable;

	public DisplaySearchOrder() {
	}

	public DisplaySearchOrder(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public Object apply(Object source) {
		if (source == null) {
			return null;
		}
		Object t = Reflections.at(source).property(fieldName).get(source);
		if (t == null) {
			return null;
		}
		if (toComparable == null) {
			toComparable = Registry.impl(OrderProvider.class).toComparable(t);
		}
		return toComparable.apply(source, t);
	}

	@Override
	public boolean equivalentTo(SearchOrder other) {
		return super.equivalentTo(other) && Objects.equals(fieldName,
				((DisplaySearchOrder) other).fieldName);
	}

	public String getFieldName() {
		return this.fieldName;
	}

	@Override
	public String provideKey() {
		return getFieldName();
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public String toString() {
		return getFieldName();
	}

	@Registration(OrderProvider.class)
	public static class OrderProvider {
		public BiFunction<Object, Object, Comparable>
				toComparable(Object nonNullFieldValue) {
			if (nonNullFieldValue instanceof Comparable) {
				return (source_, o) -> (Comparable) o;
			} else if (nonNullFieldValue instanceof Collection) {
				return (source_, o) -> ((Collection) o).size();
			} else if (nonNullFieldValue instanceof OneToManyMultipleSummary) {
				return (source_, o) -> ((OneToManyMultipleSummary) o)
						.provideSize((Entity) source_);
			} else if (nonNullFieldValue instanceof HasDisplayName) {
				return (source_, o) -> ((HasDisplayName) o).displayName();
			} else if (nonNullFieldValue instanceof Entity) {
				return (source_, o) -> ((Entity) o).getId();
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}
}
