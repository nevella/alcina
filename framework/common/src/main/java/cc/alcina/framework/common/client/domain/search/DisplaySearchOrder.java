package cc.alcina.framework.common.client.domain.search;

import java.util.Objects;
import java.util.function.BiFunction;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;

public class DisplaySearchOrder extends SearchOrder {
	private String fieldName;

	transient BiFunction<Object, Object, Comparable> toComparable;

	public transient BindableSearchDefinition searchDefinition;

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
			toComparable = Registry.impl(OrderProvider.class).toComparable(this,
					t);
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
	public interface OrderProvider {
		public BiFunction<Object, Object, Comparable> toComparable(
				DisplaySearchOrder order, Object nonNullFieldValue);
	}
}
