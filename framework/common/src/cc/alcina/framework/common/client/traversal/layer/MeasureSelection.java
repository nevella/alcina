package cc.alcina.framework.common.client.traversal.layer;

import java.util.List;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.DetachedRootSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.NestedName;

public class MeasureSelection extends AbstractSelection<Measure>
		implements Comparable<MeasureSelection> {
	/*
	 * Utility to allow usage of measurecontainment etc from non-selection
	 * measures
	 */
	public static List<MeasureSelection> fromMeasures(List<Measure> measures) {
		DetachedRootSelection root = new DetachedRootSelection();
		IntPair union = IntPair
				.unionOf(measures.stream().map(Measure::toIntPair).toList());
		Stream<Measure> stream = measures.stream();
		if (measures.stream().map(Measure::toIntPair)
				.noneMatch(p -> p.equals(union))) {
			Measure unionMeasure = Measure
					.fromRange(
							measures.iterator().next()
									.truncateAbsolute(union.i1, union.i2),
							Measure.Token.Generic.TYPE);
			stream = Stream.concat(stream, Stream.of(unionMeasure));
		}
		return stream.map(m -> new MeasureSelection(root, m)).toList();
	}

	private boolean omit;

	public MeasureSelection(Selection parent, Measure measure) {
		this(parent, measure, null);
	}

	public MeasureSelection(Selection parent, Measure measure,
			String pathSegment) {
		super(parent, measure, pathSegment);
	}

	@Override
	public int compareTo(MeasureSelection o) {
		return get().compareTo(o.get());
	}

	public boolean contains(MeasureSelection o, Token.Order order) {
		return get().contains(o.get(), order);
	}

	/**
	 * A fallback for complex ordering behaviour (generally for two selections
	 * of the same type), such as nested indents
	 */
	public int equalRangeCompare(MeasureSelection o) {
		return 0;
	}

	@Override
	public boolean isContainedBy(Selection selection) {
		if (selection instanceof MeasureSelection) {
			MeasureSelection o = (MeasureSelection) selection;
			if (o.get().contains(get())) {
				return true;
			}
		}
		return super.isContainedBy(selection);
	}

	/*
	 * Omit from output (and traversal)
	 */
	public boolean isOmit() {
		return this.omit;
	}

	public String markup() {
		return get().markup();
	}

	public void setOmit(boolean omit) {
		this.omit = omit;
	}

	public String text() {
		return get().text();
	}

	public void toDebugOut() {
		Ax.out(toDebugString());
	}

	@Override
	public String toDebugString() {
		return Ax.format("%s :: %s :: %s", super.toString(), get().getData(),
				Ax.ntrim(get().text(), 40));
	}

	public MeasureSelection truncateTo(IntPair range) {
		Ax.err("Attempting to truncate %s to %s", NestedName.get(this), range);
		throw new UnsupportedOperationException();
	}

	public interface IgnoreOverlaps {
	}

	// Not intended for final output
	public interface Intermediate {
	}

	static class View implements Selection.View<MeasureSelection> {
		@Override
		public String getDiscriminator(MeasureSelection selection) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.appendIfNotBlank(selection.get().token,
					selection.get().getData());
			return format.toString();
		}

		@Override
		public String getMarkup(MeasureSelection selection) {
			return selection.get().markup();
		}

		@Override
		public String getPathSegment(MeasureSelection selection) {
			return selection.get().toIntPair().toString();
		}

		@Override
		public String getText(MeasureSelection selection) {
			return selection.get().ntc();
		}
	}
}