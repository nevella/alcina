package cc.alcina.framework.common.client.traversal.layer;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.process.TreeProcess.HasReleaseableResources;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.DetachedRootSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.NestedName;

public class MeasureSelection extends AbstractSelection<Measure>
		implements Comparable<MeasureSelection>, Selection.WithRange<Measure>,
		HasReleaseableResources {
	/*
	 * Utility to allow usage of measurecontainment etc from non-selection
	 * measures
	 */
	public static List<MeasureSelection> fromMeasures(List<Measure> measures) {
		DetachedRootSelection root = new DetachedRootSelection();
		IntPair union = IntPair.unionOf(measures.stream()
				.map(Measure::toIntPair).collect(Collectors.toList()));
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
		return stream.map(m -> new MeasureSelection(root, m))
				.collect(Collectors.toList());
	}

	private boolean omit;

	public MeasureSelection(Selection parent, Measure measure) {
		this(parent, measure, null);
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		get().detach();
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
	public boolean matchesText(String textFilter) {
		if (TextRangeMatcher.matches(textFilter, get().toIntPair())) {
			return true;
		}
		return super.matchesText(textFilter);
	}

	/*
	 * A simple per-app cache to optimise range filtering
	 */
	static class TextRangeMatcher {
		static class Data {
			static Data last;

			String textFilter;

			int idx = -1;

			Data(String textFilter) {
				this.textFilter = textFilter;
				if (textFilter.matches("\\d{1,9}")) {
					idx = Integer.parseInt(textFilter);
				}
			}

			static Data get(String textFilter) {
				Data data = last;
				if (data != null && data.textFilter == textFilter) {
					return data;
				}
				data = new Data(textFilter);
				last = data;
				return data;
			}

			boolean matches(IntPair intPair) {
				return idx == -1 ? false : intPair.contains(idx);
			}
		}

		public static boolean matches(String textFilter, IntPair intPair) {
			return Data.get(textFilter).matches(intPair);
		}
	}

	@Override
	public String toDebugString() {
		return Ax.format("%s :: %s :: %s", super.toString(), get().getData(),
				Ax.ntrim(get().text(), 40));
	}

	public MeasureSelection truncateTo(IntPair range) {
		Ax.err("[base measure-selection - perhaps override?] Attempting to truncate %s to %s",
				NestedName.get(this), range);
		throw new UnsupportedOperationException();
	}

	public interface IgnoreOverlaps {
	}

	// Not intended for final output
	public interface Intermediate {
	}

	public static class View<M extends MeasureSelection>
			extends AbstractSelection.View<M> {
		@Override
		public String getDiscriminator(MeasureSelection selection) {
			FormatBuilder format = new FormatBuilder().separator(" - ");
			format.appendIfNotBlank(selection.get().token,
					selection.get().getData());
			return format.toString();
		}

		@Override
		public String getMarkup(MeasureSelection selection) {
			try {
				return selection.get().markup();
			} catch (Exception e) {
				e.printStackTrace();
				return "Unable to parse markup (possibly namespace issue)";
			}
		}

		@Override
		public String getPathSegment(MeasureSelection selection) {
			return selection.get().toIntPair().toString();
		}

		@Override
		public String computeText(MeasureSelection selection) {
			return selection.get().ntc();
		}
	}

	@Override
	public Range provideRange() {
		return get();
	}

	public void logContainingSelections(Token.Order order) {
		IntPair thisRange = get().toIntPair();
		List<MeasureSelection> containers = SelectionTraversal
				.contextTraversal().getSelections(MeasureSelection.class, true)
				.stream().filter(ms -> ms.get().toIntPair().contains(thisRange))
				.toList();
		new MeasureContainment(order, containers).dump();
	}
}