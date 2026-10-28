package cc.alcina.framework.servlet.transform.fragment.output;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.IndentRun;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.StructuralTokens;
import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.dom.Measure.Token.CheckDescendantContainment;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.dom.Measure.Token.RestrictedChildContainment;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Containment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Overlap;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.MeasureTreeComparator;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.entity.Configuration;

/**
 * <p>
 * Split structural run measures (e.g. MergedIndentLayer.IndentRun) that are
 * logicaly "lower" than overlapping "higher" runs (e.g. top-level structural
 * measures). Also omit a text measure if it has overlapping 'split' text
 * measures
 * <p>
 * Also (variant containment) replace an outer logical conteiner with multiple
 * inner if the outer would break browser DOM constraints (no TABLE - TBODY -
 * LOGICAL-BLOCK - TR)
 * <p>
 * This is the most re-written part of the transformer, some constraints/tasks
 * are:
 * <ul>
 * <li>Must split an indentrange if it intersects multiple logical ranges
 * ('section' etc)
 * <li>The Order comparator relation must be preserved [sic - no, not really]
 * <li>TODO - doc - link to variant container relation
 * </ul>
 */
public class MeasureSplitAndHandleTextOverlaps
		extends Layer<DocumentTransformerSelection> {
	/*
	 * Possibly dev-only, possibly not. Why should an indentrun map a set of
	 * TRs? certainly discarding is better than failing in production
	 */
	public static final Configuration.Key discardContainmentTruncations = Configuration
			.key("discardContainmentTruncations");

	public static class Peer {
		public static final Configuration.Key suppressExceptions = Configuration
				.key("suppressExceptions");

		public interface Has {
			Peer getMeasureSplitAndHandleTextOverlapsPeer();
		}

		public boolean isSuppressExceptions() {
			return Al.isProduction() || suppressExceptions.is();
		}
	}

	Multimap<MeasureSelection, List<TruncationData>> selectionTruncations = new Multimap<>();

	Order order;

	@Override
	public void onBeforeTraversal(SelectionTraversal.State traversalState) {
		super.onBeforeTraversal(traversalState);
		order = state.traversalContext(Order.Has.class).getOrder();
		performTasks();
	}

	void performTasks() {
		/*
		 * first truncate overlaps - so there's a tree structure to reason about
		 */
		truncateOverlaps();
		logger.info("child containments:");
		/*
		 * Now, reorder containments to satisfy DOM constraints
		 */
		truncateChildContainments();
		logger.info("descendant containments:");
		/*
		 * And to slice n dice say text nodes
		 */
		truncateDescendantContainments();
	}

	int truncationPass;

	void truncateChildContainments() {
		do {
			generateContainment();
			measureContainment.containments()
					.forEach(this::computeChildContainmentTruncations);
			selectionTruncations.forEach(this::computeAdditionalTruncations);
		} while (processTruncations());
	}

	void truncateDescendantContainments() {
		do {
			generateContainment();
			measureContainment.containments()
					.forEach(this::computeDescendantTruncations);
			selectionTruncations.forEach(this::computeAdditionalTruncations);
		} while (processTruncations());
	}

	void dumpTruncations() {
		Ax.out(selectionTruncations.entrySet().stream().map(Object::toString)
				.collect(Collectors.joining("\n\n")));
	}

	void truncateOverlaps() {
		generateContainment();
		measureContainment.overlaps.forEach(this::createTruncation);
		selectionTruncations.forEach(this::computeAdditionalTruncations);
		processTruncations();
	}

	void generateContainment() {
		Stream<MeasureSelection> filteredSelections = state.traversalState.selections
				.get(MeasureSelection.class, true).stream()
				.filter(m -> !m.isOmit()
						&& !(m instanceof MeasureSelection.IgnoreOverlaps));
		MeasureTreeComparator measureTreeComparator = new MeasureTreeComparator(
				order);
		List<MeasureSelection> measures = filteredSelections
				.sorted(measureTreeComparator).collect(Collectors.toList());
		measureContainment = new MeasureContainment(order, measures);
	}

	/*
	 * return true if changes were generated
	 */
	boolean processTruncations() {
		int omitInvalidChildCount = selections(OmitInvalidChild.class).size();
		int omitInvalidChildDelta = omitInvalidChildCount
				- omitInvalidChildCountEntry;
		omitInvalidChildCountEntry = omitInvalidChildCount;
		String omitInvalidChildMessage = omitInvalidChildDelta == 0 ? ""
				: Ax.format(" (%s invalid children omitted)",
						omitInvalidChildDelta);
		logger.info("truncation pass {}{}", truncationPass++,
				omitInvalidChildMessage);
		selectionTruncations.keySet().forEach(this::processTruncation);
		boolean result = selectionTruncations.size() > 0
				|| omitInvalidChildDelta > 0;
		omitInvalidChildCountEntry = omitInvalidChildCount;
		selectionTruncations.clear();
		return result;
	}

	int omitInvalidChildCountEntry = 0;

	void debugTruncations() {
		selectionTruncations.entrySet().forEach(e -> {
			Ax.out(e.getKey());
			e.getValue().forEach(v -> Ax.out("\t%s", v));
		});
	}

	Logger logger = LoggerFactory.getLogger(getClass());

	private MeasureContainment measureContainment;

	/*
	 * If a selection (once output) can't be wrapped by its parent, push the
	 * wrapper down
	 * 
	 * FIXME - fragmentDocument - gather data for when child truncations occur,
	 * and determine if they *ever* should be preserved
	 */
	void computeChildContainmentTruncations(
			MeasureContainment.Containment containment) {
		MeasureSelection selection = containment.selection;
		MeasureContainment.Containment parent = containment.parent();
		Token selectionToken = selection.token();
		if (selectionToken instanceof RestrictedChildContainment) {
			RestrictedChildContainment restrictedChildContainment = (RestrictedChildContainment) selectionToken;
			containment.getChildContainments().forEach(cc -> {
				MeasureSelection child = cc.selection;
				if (!restrictedChildContainment
						.allowChildContainment(child.token())) {
					/*
							@formatter:off

							note that the below is wrong anyway - should truncate children to be inside grandchildren.
							buuuttt... if the grandchildren are invalid, it really gets tricky

							for the moment, going with omit 

					List<Containment> grandchildContainments = cc
							.getChildContainments();
					Containment firstGrandchild = grandchildContainments.get(0);
					selectionTruncations.add(selection, new TruncationData(
							child.get().toIntPair(), selection, child));
							@formatter:on
							 */
					child.setOmit(true);
					Measure measure = Measure.fromRange(child.get(),
							StructuralTokens.OmitInvalidChild.TYPE);
					OmitInvalidChild omit = new OmitInvalidChild(child,
							measure);
					// and, of course, omit
					omit.setOmit(true);
					select(omit);
				}
			});
			/*
			 * Simply truncate. Because computeTruncations() loops, the
			 * truncations will themselves be truncated until a valid
			 * containment state is reached
			 */
		}
	}

	/*
	 * This might need revisiting - now things have been extended with
	 * table/tbody/tr child logic
	 * 
	 * First step is to check a few examples. One main reason for this is (I
	 * think) that if a footnote measure is a containment child of a span/text,
	 * this splits the span/text for nice containment
	 * 
	 * The difference to childcontaiments is that childcontainments
	 * "pushes down" changes in a loop - this algorithm can jump layers in the
	 * containment tree
	 */
	void computeDescendantTruncations(
			MeasureContainment.Containment containment) {
		MeasureSelection selection = containment.selection;
		MeasureContainment.Containment parent = containment.parent();
		Token selectionToken = selection.token();
		if (selectionToken instanceof CheckDescendantContainment) {
			CheckDescendantContainment checksContainment = (CheckDescendantContainment) selectionToken;
			containment.descendants.forEach(desc -> {
				int cmp = order.compare(selectionToken, desc.get().token);
				if (cmp > 0) {
					/*
					 * Any token that reaches here must implement
					 * CheckDescendantContainment, even if only to say (as for
					 * Text) "no!"
					 */
					Token.CheckDescendantContainment check = (CheckDescendantContainment) selectionToken;
					if (check.allowDescendantContainment(desc.get())) {
						// do not truncate
					} else {
						/*
						 * the measureSelection selection cannot contain
						 * descendant desc, so add a truncation of selection to
						 * the list
						 */
						IntPair descPair = desc.get().toIntPair();
						beforePutTruncation(selection);
						selectionTruncations.add(selection,
								new TruncationData(descPair, selection, desc));
					}
				}
			});
		}
	}

	void computeAdditionalTruncations(MeasureSelection selection,
			List<TruncationData> truncationList) {
		/*
		 * simple implementation, for nested overlaps this will need to dig
		 * deeper
		 * 
		 * 
		 * dedupe removes the simplest nesting
		 */
		List<IntPair> initialList = CommonUtils
				.dedupe(truncationList.stream().map(td -> td.truncateTo)
						.sorted().collect(Collectors.toList()));
		List<IntPair> measureList = initialList;
		boolean hasNestedTruncations = false;
		for (int idx = 0; idx < measureList.size() - 1; idx++) {
			IntPair p1 = measureList.get(idx);
			IntPair p2 = measureList.get(idx + 1);
			if (p1.intersectsWithNonPoint(p2)) {
				hasNestedTruncations = true;
				break;
			}
		}
		if (hasNestedTruncations) {
			// check there are no overlaps in the truncation structure
			List<MeasureSelection> truncatingMeasures = CommonUtils
					.dedupe(truncationList.stream().map(td -> td.cause).sorted()
							.collect(Collectors.toList()));
			MeasureContainment measureContainment = new MeasureContainment(
					order, truncatingMeasures);
			Preconditions.checkState(measureContainment.overlaps.isEmpty());
			List<Containment> containments = measureContainment.containments()
					.collect(Collectors.toList());
			measureList = containments.stream()
					.flatMap(Containment::toNonChildRanges).distinct().sorted()
					.collect(Collectors.toList());
			if (!IntPair.isContinuous(measureList)) {
				// FIXME - fragmentDocument
				logger.warn(("DEVEX-1 discontinuous selection truncations"));
				Ax.err(selection);
				Ax.err(measureList);
				Peer peer = state.traversalContext(Peer.Has.class)
						.getMeasureSplitAndHandleTextOverlapsPeer();
				if (!peer.isSuppressExceptions()) {
					throw new IllegalStateException(
							"Selection truncations are not continuous");
				}
			}
		}
		List<IntPair> additionalSplits = IntPair.provideUncovered(measureList,
				selection.get().toIntPair());
		List<IntPair> merged = new ArrayList<>();
		merged.addAll(measureList);
		merged.addAll(additionalSplits);
		Collections.sort(merged);
		beforePutTruncation(selection);
		selectionTruncations.put(selection, merged.stream().map(pair -> {
			int initialIndex = initialList.indexOf(pair);
			MeasureSelection cause = initialIndex == -1 ? null
					: truncationList.get(initialIndex).cause;
			return new TruncationData(pair, selection, cause);
		}).collect(Collectors.toList()));
	}

	void beforePutTruncation(MeasureSelection selection) {
	}

	boolean dumpedContainment = false;

	@Override
	public void process(DocumentTransformerSelection selection)
			throws Exception {
		// NOOP
	}

	/*
	 * Split the selection into several measures, to allow tree recombination
	 */
	void processTruncation(MeasureSelection selection) {
		if (selectionTruncations.containsKey(selection)) {
			List<TruncationData> truncations = selectionTruncations
					.get(selection);
			try {
				logger.debug("\t{}", selection);
				truncations.stream()
						.forEach(t -> logger.debug("\t\t -> {}", t));
				List<IntPair> truncateTos = truncations.stream()
						.map(t -> t.truncateTo).toList();
				List<IntPair> splits = IntPair.asContinuousRangeList(
						selection.get().toIntPair(), truncateTos);
				splits.forEach(pair -> {
					MeasureSelection truncated = selection.truncateTo(pair);
					if (truncated != null) {
						select(truncated);
					}
				});
				selection.setOmit(true);
			} catch (RuntimeException e) {
				logger.warn(
						"Selection truncation issue: \n\tTruncating: {}\n\tTo:\n\t{}",
						selection, truncations.stream().map(Object::toString)
								.collect(Collectors.joining("\n\t")));
				if (!dumpedContainment) {
					dumpedContainment = true;
					Ax.sysLogHigh("Dumping measure containment");
					measureContainment.dump();
					Ax.out("\n\n===================================\n\n");
				}
				throw e;
			}
		}
	}

	/*
	 * Models omission of the parent selection due to that selection being an
	 * invalid containment child of its tree parent
	 */
	static class OmitInvalidChild extends MeasureSelection {
		OmitInvalidChild(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	class TruncationData {
		IntPair truncateTo;

		MeasureSelection truncate;

		MeasureSelection cause;

		public TruncationData(IntPair truncateTo, MeasureSelection truncate,
				MeasureSelection cause) {
			this.truncateTo = truncateTo;
			this.truncate = truncate;
			this.cause = cause;
		}

		@Override
		public boolean equals(Object obj) {
			TruncationData o = (TruncationData) obj;
			return CommonUtils.equals(truncateTo, o.truncateTo, truncate,
					o.truncate);
		}

		@Override
		public int hashCode() {
			return Objects.hash(truncateTo, truncate);
		}

		@Override
		public String toString() {
			return Ax.format("%s ==> %s [%s]", truncate, truncateTo,
					cause == null ? "no measure" : NestedName.get(cause));
		}
	}

	void createTruncation(Overlap overlap) {
		Measure.Token t1 = overlap.s1.get().token;
		Measure.Token t2 = overlap.s2.get().token;
		int cmp = order.compare(t1, t2);
		if (cmp == 0) {
			boolean useIndexOrdering = false;
			if (t1 == t2) {
				/*
				 * FIXME - st.bn - example is au-leg-act C2014A00081
				 */
				if (t1 == IndentRun.TYPE) {
					useIndexOrdering = true;
				}
			}
			if (useIndexOrdering) {
				cmp = overlap.s1.get().toIntPair().i1
						- overlap.s2.get().toIntPair().i1;
			}
		}
		if (cmp == 0) {
			DomDocument sourceDocument = overlap.s1.get()
					.startContainingNode().document;
			sourceDocument.locations().validateLocations();
			overlap.s1.get().log();
			String message = Ax.format(
					"Unable to order overlapping selections :: %s - %s :: %s - %s",
					t1, t2, overlap.s1.get().toIntPair(),
					overlap.s2.get().toIntPair());
			throw new IllegalStateException(message);
		}
		MeasureSelection keep = cmp < 0 ? overlap.s1 : overlap.s2;
		MeasureSelection truncate = cmp < 0 ? overlap.s2 : overlap.s1;
		beforePutTruncation(truncate);
		selectionTruncations.add(truncate,
				new TruncationData(overlap.intersection, truncate, keep));
	}
}