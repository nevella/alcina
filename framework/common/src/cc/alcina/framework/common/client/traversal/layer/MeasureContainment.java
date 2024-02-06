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

import cc.alcina.framework.common.client.traversal.layer.Measure.Token.Order;
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
	public class Containment implements Comparable<Containment> {
		public MeasureSelection selection;

		public <S extends MeasureSelection> S typedSelection() {
			return (S) selection;
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

		boolean isImmediateChild(MeasureSelection selection) {
			return containments.get(selection).ancestors(false).findFirst()
					.orElse(null) == this;
		}

		public List<MeasureSelection> descendants = new ArrayList<>();

		List<MeasureSelection> containers = new ArrayList<>();

		Containment(MeasureSelection selection) {
			this.selection = selection;
		}

		public boolean isContainedBy(MeasureSelection selection) {
			return ancestors(false).anyMatch(c -> c.selection == selection);
		}

		public boolean isToken(Measure.Token token) {
			return selection.get().token == token;
		}

		public boolean isContainedBy(Measure.Token token) {
			return ancestors(false)
					.anyMatch(c -> c.selection.get().token == token);
		}

		public MeasureSelection soleContained(Measure.Token token) {
			return descendants(false)
					.filter(c -> c.selection.get().token == token).findFirst()
					.get().selection;
		}

		public Containment parent() {
			return ancestors(false).findFirst().orElse(null);
		}

		Stream<Containment> ancestors(boolean includeSelf) {
			Set<Containment> ancestorList = AlcinaCollections
					.newLinkedHashSet();
			if (includeSelf) {
				ancestorList.add(this);
			}
			Set<Containment> pending = AlcinaCollections.newLinkedHashSet();
			pending.add(this);
			while (pending.size() > 0) {
				Iterator<Containment> itr = pending.iterator();
				Containment next = itr.next();
				itr.remove();
				next.containers.forEach(c -> {
					Containment ancestorContainment = containments.get(c);
					ancestorList.add(ancestorContainment);
					pending.add(ancestorContainment);
				});
			}
			return ancestorList.stream().sorted();
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

		/*
		 * A cache of immediate children, ordered by start (since guaranteed no
		 * overlaps)
		 */
		List<MeasureSelection> immediateChildren;

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

		void ensureImmediateChildSelections() {
			if (immediateChildren == null) {
				immediateChildren = descendants.stream()
						.filter(this::isImmediateChild).sorted().toList();
			}
		}

		public List<Containment> getChildContainments() {
			ensureImmediateChildSelections();
			return immediateChildren.stream().map(containments::get).toList();
		}

		@Override
		public String toString() {
			return Ax.format("Containment: %s", selection);
		}

		public int depth() {
			return (int) ancestors(false).count();
		}
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

	public static class ContainmentMap<T extends MeasureSelection> {
		Order order;

		MeasureContainment containment;

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
					if (openRange.contains(cursorRange)) {
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

	Map<MeasureSelection, Containment> containments = AlcinaCollections
			.newLinkedHashMap();

	public List<Overlap> overlaps = new ArrayList<>();

	List<MeasureSelection> openSelections = new LinkedList<>();

	Containment root;

	public MeasureContainment(Measure.Token.Order order,
			Collection<? extends MeasureSelection> selections) {
		MeasureTreeComparator comparator = new MeasureTreeComparator(
				// this will also remove overlapping text nodes, so we
				// need to relax a comparator constraint
				order.copy().withIgnoreNoPossibleChildren());
		List<MeasureSelection> measures = selections.stream().sorted(comparator)
				.collect(Collectors.toList());
		ContainmentComputation computation = new ContainmentComputation(
				measures);
		computation.compute();
		root = containments.values().stream().filter(c -> c.parent() == null)
				.findFirst().get();
	}

	public Stream<Containment> containments() {
		return containments.values().stream();
	}
}
