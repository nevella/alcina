package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.servlet.transform.fragment.DocumentElementLayer.DocumentElement;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.pos.IndentRootLayer.IndentRoot;
import cc.alcina.framework.servlet.transform.fragment.pos.ListItemLabelLayer.ListItemLabel;
import cc.alcina.framework.servlet.transform.fragment.pos.MergedIndentLayer.IndentRun;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Containment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.ContainmentMap;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;

/*
 * Divide list runs into list items, using the labels as start markers
 * 
 * This is 2-phase - first build the label run lookup, then generate the run
 * layer
 */
class ListItemLayer extends Layer<ListItemLabel> {
	// NDIS hack
	Set<IntPair> existingRanges = new LinkedHashSet<>();

	Multimap<IndentRun, List<ListItemLabel>> labelRuns = new Multimap<>();
	/*
	 * note that this is key - indentruns don't necessarily form a stable
	 * containment - if say a doc ends in a table - but those corresponding to a
	 * given IndentRoot *do*
	 */

	/*
	 * lazy for performance reasons
	 */
	Map<IndentRoot, MeasureContainment.ContainmentMap<IndentRun>> indentRuns = new LinkedHashMap<>();

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		Order order = state.traversalContext(Order.Has.class).getOrder();
		/*
		 * note that this is key - indentruns don't necessarily form a stable
		 * containment - if say a doc ends in a table, but those corresponding
		 * to a given IndentRoot *do*
		 */
		indentRoots = new MeasureContainment.ContainmentMap<IndentRoot>(order,
				selections(IndentRoot.class));
	}

	MeasureContainment.ContainmentMap<IndentRun>
			getIndentRunContainment(IndentRoot root) {
		Order order = state.traversalContext(Order.Has.class).getOrder();
		return indentRuns.computeIfAbsent(root, root0 -> {
			return new MeasureContainment.ContainmentMap<IndentRun>(order,
					selections(IndentRun.class).stream()
							.filter(run -> root0.get().contains(run.get()))
							.toList());
		});
	}

	MeasureContainment.ContainmentMap<IndentRoot> indentRoots;

	@Override
	protected void onAfterIteration() {
		labelRuns.forEach((run, labels) -> {
			int size = labels.size();
			for (int idx = 0; idx < size; idx++) {
				// this is at least
				ListItemLabel logicalParent = labels.get(idx);
				if (idx == 0) {
					run.firstListItem = logicalParent;
				}
				StyledNode blockStart = logicalParent.blockStart;
				/*
				 * These measures need to be aligned with the containing block
				 * node - otherwise measuresplit will have issues when pushing
				 * down the containing block - so the locations are wrapped in
				 * blockLocation
				 */
				Location start = blockStart.get().asLocation();
				Location end = idx == size - 1 ? run.get().end
						: labels.get(idx + 1).blockStart.get().asLocation();
				IntPair intPair = new IntPair(start.getIndex(), end.getIndex());
				if (existingRanges.add(intPair)) {
					Measure measure = new Measure(start, end,
							LayoutTokens.ListItem.TYPE);
					select(new ListItem(logicalParent, measure));
					Location contentsStart = logicalParent.get().end
							.toStartLocation();
					/*
					 * actually, the range is start...end - the label measure is
					 * going to be omitted anyway. otherwise there are probs
					 */
					Measure contents = new Measure(contentsStart, end,
							LayoutTokens.ListItemContents.TYPE);
					select(new ListItemContents(logicalParent, contents));
				} else {
					Ax.out("Indent run -> ignore :: %s", intPair);
				}
			}
		});
		patchOverlaps();
		super.onAfterIteration();
	}

	/*
	 * This handles e.g. a really nasty case where a note has the same indent as
	 * part of an li structure
	 * 
	 * Procedure - build a contaiment map of ListItem measures, which will give
	 * us the overlaps. Truncate, and iterate
	 * 
	 * FIXME - FdGeneric
	 */
	void patchOverlaps() {
		Order order = state.traversalContext(Order.Has.class).getOrder();
		List<MeasureSelection> list = new ArrayList<>();
		selections(ListItem.class).stream().filter(s -> !s.isOmit())
				.forEach(list::add);
		// containment maps need a root, so use the DocumentElement selection
		list.add(selection(DocumentElement.class));
		ContainmentMap<MeasureSelection> overlapCheck = new MeasureContainment.ContainmentMap<>(
				order, list);
		if (overlapCheck.containment.overlaps.isEmpty()) {
			return;
		}
		ContainmentMap<StyledMeasureSelection> fullContainment = new MeasureContainment.ContainmentMap<>(
				order, selectionsWithSubtypes(StyledMeasureSelection.class));
		overlapCheck.containment.overlaps.forEach(overlap -> {
			// naive fix policy, remove the first
			MeasureSelection removeLi = overlap.s1;
			Containment containment = fullContainment
					.getLowestContainment(removeLi, true);
			// this will be the lowest containment, ascend to the ListItem
			containment = containment.ancestors(true)
					.filter(ct -> ct.selection instanceof ListItem)
					.reduce(Ax.last()).get();
			omitIfListItemStructure(removeLi);
			containment.descendants.forEach(this::omitIfListItemStructure);
		});
		// recurse
		patchOverlaps();
	}

	void omitIfListItemStructure(MeasureSelection selection) {
		boolean listItemStructure = selection instanceof ListItemContents
				|| selection instanceof ListItem
				|| selection instanceof ListItemLabel;
		if (listItemStructure) {
			logger.warn("omitIfListItemStructure :: {}", selection);
			selection.setOmit(true);
		}
	}

	@Override
	public void process(ListItemLabel label) throws Exception {
		IndentRoot root = indentRoots.getLowestContainingMeasure(label, false);
		IndentRun run = getIndentRunContainment(root)
				.getLowestContainingMeasure(label, false);
		if (!run.isList()) {
			// FIXME - list in table cell
			// Ax.out("Indent run -> label not in list run :: %s", label);
			return;
		} else {
			labelRuns.add(run, label);
		}
	}

	static class ListItem extends StyledMeasureSelection {
		ListItem(ListItemLabel parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new ListItem((ListItemLabel) parentSelection(), measure);
		}
	}

	static class ListItemContents extends StyledMeasureSelection {
		ListItemContents(ListItemLabel parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new ListItemContents((ListItemLabel) parentSelection(),
					measure);
		}
	}
}