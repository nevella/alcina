package cc.alcina.framework.gwt.client.logic.domain.search;

import java.util.Collection;
import java.util.function.BiFunction;

import cc.alcina.framework.common.client.domain.search.DisplaySearchOrder;
import cc.alcina.framework.common.client.domain.search.DisplaySearchOrder.OrderProvider;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.logic.domain.EntityDataObject.OneToManyMultipleSummary;

@Registration(OrderProvider.class)
public class OrderProviderImpl implements DisplaySearchOrder.OrderProvider {
	public BiFunction<Object, Object, Comparable>
			toComparable(DisplaySearchOrder order, Object nonNullFieldValue) {
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