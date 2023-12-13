package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * Models (at a point in time - a stage in traersal, so this is a model of a
 * snapshot, not live) measure containments and overlaps.
 * 
 * Per-measure containments are an ancestor structure of containing measures -
 * overlaps are (possibly multiple per measureselection) pairs of
 * measureselections with overlaps
 */
public class MeasureContainment {
	Map<MeasureSelection, Containment> containments = AlcinaCollections
			.newLinkedHashMap();

	public class Containment {
		public MeasureSelection selection;

		List<MeasureSelection> descendants = new ArrayList<>();

		List<MeasureSelection> containers = new ArrayList<>();

		Containment(MeasureSelection selection) {
			this.selection = selection;
		}

		public boolean isContainedBy(MeasureSelection selection) {
			return ancestors(false).anyMatch(c -> c.selection == selection);
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
			return ancestorList.stream();
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
			return descendantList.stream();
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
	}

	public class Overlap {
		MeasureSelection s1;

		IntPair intersection;

		int length() {
			return intersection.length();
		}

		Overlap(MeasureSelection s1, MeasureSelection s2) {
			this.s1 = s1;
			this.s2 = s2;
			intersection = s1.get().toIntPair()
					.intersection(s2.get().toIntPair());
		}

		MeasureSelection s2;
	}

	List<Overlap> overlaps = new ArrayList<>();

	MeasureContainment(Measure.Token.Order order,
			List<MeasureSelection> selections) {
		MeasureTreeComparator comparator = new MeasureTreeComparator(
				// this will also remove overlapping text nodes, so we
				// need to relax a comparator constraint
				order.copy().withIgnoreNoPossibleChildren());
		List<MeasureSelection> measures = selections.stream().sorted(comparator)
				.collect(Collectors.toList());
		ContainmentComputation computation = new ContainmentComputation(
				measures);
		computation.compute();
	}

	class ContainmentComputation {
		List<Overlap> overlaps = new ArrayList<>();

		List<MeasureSelection> selections;

		ContainmentComputation(List<MeasureSelection> selections) {
			this.selections = selections;
		}

		List<MeasureSelection> openSelections = new LinkedList<>();

		/*
		 * Relies on the initial ordering of the selections - an overlap being
		 * [A,B] :: [C,D] where A<C, B>C,B<D
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
					if (openRange.contains(cursorRange)) {
						containment.containers.add(open);
						containments.get(open).descendants.add(cursor);
					}
					if (cursorRange.intersectsWithNonPoint(openRange)) {
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

	List<MeasureSelection> openSelections = new LinkedList<>();

	public Stream<Containment> containments() {
		return containments.values().stream();
	}
}
