package cc.alcina.framework.common.client.domain.search;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;
import cc.alcina.framework.common.client.util.HasDisplayName;

public class DisplaySearchOrder extends SearchOrder {
	private String fieldName;

	public String getFieldName() {
		return this.fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	transient BiFunction<Object, Object, Comparable> toComparable;

	@Override
	public Object apply(Object source) {
		if (source == null) {
			return null;
		}
		Object t = Reflections.propertyAccessor().getPropertyValue(source,
				fieldName);
		if (t == null) {
			return null;
		}
		if (toComparable == null) {
			if (t instanceof Comparable) {
				toComparable = (source_, o) -> (Comparable) o;
			} else if (t instanceof Collection) {
				toComparable = (source_, o) -> ((Collection) o).size();
			} else if (t instanceof OneToManyMultipleSummary) {
				toComparable = (source_, o) -> ((OneToManyMultipleSummary) o)
						.provideSize((Entity) source_);
			} else if (t instanceof HasDisplayName) {
				toComparable = (source_, o) -> ((HasDisplayName) o)
						.displayName();
			} else if (t instanceof Entity) {
				toComparable = (source_, o) -> ((Entity) o).getId();
			} else {
				throw new UnsupportedOperationException();
			}
		}
		return toComparable.apply(source, t);
	}

	@Override
	public String provideKey() {
		return getFieldName();
	}

	@Override
	public boolean equivalentTo(SearchOrder other) {
		return super.equivalentTo(other) && Objects.equals(fieldName,
				((DisplaySearchOrder) other).fieldName);
	}

	@Override
	public String toString() {
		return getFieldName();
	}
}
