package cc.alcina.framework.common.client.traversal.layer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.util.Ax;

/**
 * A class which derives additional structural measure from preceding measures
 */
public abstract class MeasureLayer<S extends MeasureSelection>
		extends Layer<S> {
	protected MeasureContainment measureContainment() {
		Stream<MeasureSelection> filteredSelections = state.traversalState.selections
				.get(MeasureSelection.class, true).stream()
				.filter(m -> !m.isOmit()
						&& !(m instanceof MeasureSelection.IgnoreOverlaps));
		Order order = state.traversalContext(Order.Has.class).getOrder();
		List<MeasureSelection> measures = filteredSelections
				.sorted(new MeasureTreeComparator(order))
				.collect(Collectors.toList());
		return new MeasureContainment(order, measures);
	}

	protected <S1 extends MeasureSelection> S1 measureSelection(Class<S1> clazz,
			Measure.Token token) {
		List<S1> list = measureSelections(clazz, token)
				.collect(Collectors.toList());
		Preconditions.checkState(list.size() == 1);
		return Ax.first(list);
	}

	protected <S1 extends MeasureSelection> Stream<S1>
			measureSelections(Class<S1> clazz, Measure.Token token) {
		return state.traversalState.selections.get(clazz).stream()
				.filter(m -> m.get().token == token);
	}
}
