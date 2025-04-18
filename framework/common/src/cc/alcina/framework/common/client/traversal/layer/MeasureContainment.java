package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Overlap;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * Models (at a point in time - a stage in traersal, so this is a model of a
 * snapshot, not live) measure containments and overlaps.
 * 
 * Per-measure containments are an ancestor structure of containing measures -
 * overlaps are (possibly multiple per measureselection) pairs of
 * measureselections with overlaps
 */
public class MeasureContainment {
	public Map<MeasureSelection, Containment> containments = AlcinaCollections
			.newLinkedHashMap();

	public List<Overlap> overlaps = new ArrayList<>();

	Containment root;

	List<MeasureSelection> measures;

	Order order;

	public MeasureContainment(Measure.Token.Order order,
			Collection<? extends MeasureSelection> selections) {
		this.order = order;
		MeasureTreeComparator comparator = new MeasureTreeComparator(order);
		measures = selections.stream().sorted(comparator)
				.collect(Collectors.toList());
		ContainmentComputation computation = new ContainmentComputation(
				measures);
		computation.compute();
		root = containments.values().stream().filter(c -> c.parent() == null)
				.findFirst().orElse(null);
	}

	public Stream<Containment> containments() {
		return containments.values().stream();
	}

	public void dump() {
		FormatBuilder format = new FormatBuilder();
		DepthFirstTraversal<Containment> traversal = new DepthFirstTraversal<>(
				root, Containment::getChildContainments);
		traversal.forEach(c -> {
			format.indent(2 * c.depth());
			format.line("%s :: %s", c.selection,
					Ax.trim(Ax.ntrim(c.selection.get().text()), 25));
		});
		Ax.out(format);
	}

	public class Containment implements Comparable<Containment> {
		public MeasureSelection selection;

		public List<MeasureSelection> descendants = new ArrayList<>();

		/*
		 * measures which at least partially contain this selection
		 */
		List<MeasureSelection> containers = new ArrayList<>();

		/*
		 * A cache of immediate children, ordered by start (since guaranteed no
		 * overlaps)
		 */
		List<MeasureSelection> immediateChildren;

		Containment(MeasureSelection selection) {
			this.selection = selection;
		}

		public Stream<Containment> ancestors(boolean includeSelf) {
			Set<Containment> ancestorList = AlcinaCollections
					.newLinkedHashSet();
			if (includeSelf) {
				ancestorList.add(this);
			}
			// the visited check prevents massive indent structures (due to
			// broken markup) from causing exponential cost
			Set<Containment> visited = AlcinaCollections.newLinkedHashSet();
			Set<Containment> pending = AlcinaCollections.newLinkedHashSet();
			pending.add(this);
			visited.add(this);
			while (pending.size() > 0) {
				Iterator<Containment> itr = pending.iterator();
				Containment next = itr.next();
				itr.remove();
				for (MeasureSelection containingSelection : next.containers) {
					Containment ancestorContainment = containments
							.get(containingSelection);
					ancestorList.add(ancestorContainment);
					if (!visited.add(ancestorContainment)) {
						pending.add(ancestorContainment);
					}
				}
			}
			return ancestorList.stream().sorted();
		}

		/*
		 * ordered from lowest in containment hierarchy to highest
		 */
		@Override
		public int compareTo(Containment o) {
			// return the reverse of the measure comparison (containing before
			// contained)
			int cmp = selection.get().compareTo(o.selection.get());
			if (cmp == 0) {
				// ensure order is stable, if ranges are equal (earlier contains
				// later)
				cmp = selection.ensureSegmentCounter()
						- o.selection.ensureSegmentCounter();
			}
			return -cmp;
		}

		public int depth() {
			return (int) ancestors(false).count();
		}

		Stream<Containment> descendants(boolean includeSelf) {
			Set<Containment> descendantList = AlcinaCollections
					.newLinkedHashSet();
			if (includeSelf) {
				descendantList.add(this);
			}
			Set<Containment> pending = AlcinaCollections.newLinkedHashSet();
			pending.add(this);
			while (pending.size() > 0) {
				Iterator<Containment> itr = pending.iterator();
				Containment next = itr.next();
				itr.remove();
				next.descendants.forEach(c -> {
					Containment descendantContainment = containments.get(c);
					descendantList.add(descendantContainment);
					pending.add(descendantContainment);
				});
			}
			return descendantList.stream().sorted();
		}

		public List<MeasureSelection> ensureImmediateChildSelections() {
			if (immediateChildren == null) {
				immediateChildren = descendants.stream()
						.filter(this::isImmediateChild).sorted()
						.collect(Collectors.toList());
			}
			return immediateChildren;
		}

		public List<Containment> getChildContainments() {
			return ensureImmediateChildSelections().stream()
					.map(containments::get).collect(Collectors.toList());
		}

		public Containment
				getImmediateChildContaining(MeasureSelection contained) {
			if (descendants.isEmpty()) {
				return null;
			}
			MeasureSelection containing = null;
			ensureImmediateChildSelections();
			int index = Collections.binarySearch(immediateChildren, contained,
					Comparator.naturalOrder());
			if (index >= 0) {
				containing = immediateChildren.get(index);
			} else {
				int test = -index - 1;
				if (test > 0) {
					test = test - 1;
				}
				containing = immediateChildren.get(test);
			}
			if (!containing.get().contains(contained.get())) {
				containing = null;
			}
			return containments.get(containing);
		}

		public boolean isContainedBy(Measure.Token token) {
			return ancestors(false)
					.anyMatch(c -> c.selection.get().token == token);
		}

		public boolean isContainedBy(MeasureSelection selection) {
			return ancestors(false).anyMatch(c -> c.selection == selection);
		}

		boolean isImmediateChild(MeasureSelection selection) {
			return containments.get(selection).ancestors(false).findFirst()
					.orElse(null) == this;
		}

		public boolean isToken(Measure.Token token) {
			return selection.get().token == token;
		}

		public Containment parent() {
			return ancestors(false).findFirst().orElse(null);
		}

		public MeasureSelection soleContained(Measure.Token token) {
			return descendants(false)
					.filter(c -> c.selection.get().token == token).findFirst()
					.get().selection;
		}

		// generate a list of contained ranges derived from parent
		// non-intersection [union of children]. It will be unsorted. It will
		// not contain ranges corresponding to containment children
		public Stream<IntPair> toNonChildRanges() {
			IntPair self = selection.get().toIntPair();
			if (descendants.isEmpty()) {
				return Stream.of(self);
			}
			List<IntPair> childPairs = descendants.stream()
					.filter(this::isImmediateChild)
					.map(d -> d.get().toIntPair()).collect(Collectors.toList());
			IntPair childCoverage = new IntPair(Ax.first(childPairs).i1,
					Ax.last(childPairs).i2);
			List<IntPair> uncoveredInChildArea = IntPair
					.provideUncovered(childPairs, childCoverage);
			List<IntPair> uncoveredExChildArea = IntPair
					.provideUncovered(List.of(childCoverage), self);
			return Stream.concat(uncoveredExChildArea.stream(),
					uncoveredInChildArea.stream());
		}

		@Override
		public String toString() {
			return Ax.format("Containment: %s", selection);
		}

		public <S extends MeasureSelection> S typedSelection() {
			return (S) selection;
		}
	}

	class ContainmentComputation {
		List<MeasureSelection> selections;

		List<MeasureSelection> openSelections = new LinkedList<>();

		ContainmentComputation(List<MeasureSelection> selections) {
			this.selections = selections;
		}

		/*
		 * Overlap computation relies on the initial ordering of the selections
		 * - an overlap being [A,B] :: [C,D] where A<C, B>C,B<D
		 * 
		 * Note that openSelections will not always be in strict containment
		 * order
		 */
		void compute() {
			for (MeasureSelection cursor : selections) {
				Iterator<MeasureSelection> openItr = openSelections.iterator();
				Containment containment = new Containment(cursor);
				containments.put(cursor, containment);
				while (openItr.hasNext()) {
					MeasureSelection open = openItr.next();
					IntPair openRange = open.get().toIntPair();
					IntPair cursorRange = cursor.get().toIntPair();
					Containment openContainment = containments.get(open);
					int openCmpCursor = order.compare(open.get().token,
							cursor.get().token);
					// as with MeasureTreeComparator, boundaries are special if
					// one measure is a point -
					if (IntPair.sameStartAndAtLeastOnePoint(openRange,
							cursorRange) && openRange.isPoint()) {
						/*
						 * because the selections are ordered, cursorRange
						 * closes openRange
						 */
						openItr.remove();
					} else if (IntPair.sameEndAndAtLeastOnePoint(openRange,
							cursorRange) && cursorRange.isPoint()
							&& openCmpCursor > 0) {
						/*
						 * point selection (at end) closes open
						 */
						openItr.remove();
					} else if (openRange.contains(cursorRange)) {
						containment.containers.add(open);
						openContainment.descendants.add(cursor);
					} else if (cursorRange
							.containsExAtLeastOneBoundary(openRange)) {
						containment.descendants.add(open);
						openContainment.containers.add(cursor);
					} else if (cursorRange.intersectsWithNonPoint(openRange)) {
						if (cursorRange.overlapsWith(openRange)) {
							overlaps.add(new Overlap(open, cursor));
						}
					} else {
						openItr.remove();
					}
				}
				openSelections.add(cursor);
			}
		}
	}

	public static class ContainmentMap<T extends MeasureSelection> {
		Order order;

		public MeasureContainment containment;

		public ContainmentMap(Measure.Token.Order order,
				Collection<T> selections) {
			this.order = order;
			this.containment = new MeasureContainment(order, selections);
		}

		public void dump() {
			containment.dump();
		}

		/**
		 * Return the most specific container for contained. Note that the map
		 * inputs may be nested
		 * 
		 * @param contained
		 *            the contained measure
		 * @param includeSelf
		 *            accept 'contained' as a result
		 * @return the lowest containing measure
		 */
		public T getLowestContainingMeasure(MeasureSelection contained,
				boolean includeSelf) {
			return (T) getLowestContainment(contained, includeSelf).selection;
		}

		public Containment getLowestContainment(MeasureSelection contained,
				boolean includeSelf) {
			Containment cursor = containment.root;
			while (true) {
				Containment next = cursor
						.getImmediateChildContaining(contained);
				if (next == null
						|| (next.selection == contained && !includeSelf)) {
					return cursor;
				} else {
					cursor = next;
				}
			}
		}
	}

	public class Overlap {
		public MeasureSelection s1;

		public IntPair intersection;

		public MeasureSelection s2;

		Overlap(MeasureSelection s1, MeasureSelection s2) {
			this.s1 = s1;
			this.s2 = s2;
			intersection = s1.get().toIntPair()
					.intersection(s2.get().toIntPair());
		}

		int length() {
			return intersection.length();
		}

		@Override
		public String toString() {
			return Ax.format("Overlap :: %s :: %s :: %s", intersection, s1, s2);
		}
	}
}
