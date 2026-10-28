package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection.NonIndentContainer;
import cc.alcina.framework.servlet.transform.fragment.html.TableLayer;
import cc.alcina.framework.servlet.transform.fragment.pos.MergedIndentLayer.IndentRun;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Containment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.ContainmentMap;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

/**
 * 
 * <p>
 * This is possibly the most customisable layer, given that indents are
 * fundamentally complicated (mapping from a flat word structure to nested FD)
 * 
 * <p>
 * Note this should be split (normalisation is a per-context/corpus choice and
 * should be supported here, fixups factored out to InvalidDomCleanup (TODO
 * fragmentDocument)
 */
public class OutputIndentNormalisation
		extends Layer<DocumentTransformerSelection> {
	MeasureContainment.ContainmentMap<IndentRun> indentRuns;

	private List<IndentNormalisation> indentNormalisations;

	private Peer peer;

	public OutputIndentNormalisation() {
	}

	@Override
	public void process(DocumentTransformerSelection selection)
			throws Exception {
		Order order = state.traversalContext(Order.Has.class).getOrder();
		indentRuns = new MeasureContainment.ContainmentMap<>(order,
				selections(IndentRun.class));
		peer = state.traversalContext(Peer.Has.class)
				.getOutputIndentNormalisationPeer();
		indentNormalisations = peer.getIndentNormalisations();
		selections(IndentRun.class).forEach(run -> indentNormalisations
				.forEach(norm -> this.normalise(norm, run)));
		/*
		 * Where an IndentRun is the child of a TR and matches its span exactly,
		 * omit. TODO - transfer to td child
		 */
		List<MeasureSelection> tableIndents = new ArrayList<>();
		selections(IndentRun.class).forEach(tableIndents::add);
		selections(TableLayer.TableRowSelection.class)
				.forEach(tableIndents::add);
		selections(TableLayer.TableHeaderCellSelection.class)
				.forEach(tableIndents::add);
		selections(TableLayer.TableSelection.class).forEach(tableIndents::add);
		selections(TableLayer.TableBodySelection.class)
				.forEach(tableIndents::add);
		selections(TableLayer.TableHeadSelection.class)
				.forEach(tableIndents::add);
		selections(TableLayer.TableCellSelection.class)
				.forEach(tableIndents::add);
		ContainmentMap trIndentContainment = new MeasureContainment.ContainmentMap<>(
				order, tableIndents);
		trIndentContainment.containment.containments()
				.filter(ct -> ct.selection instanceof IndentRun).forEach(ct -> {
					List<Containment> list = ct.ancestors(false).toList();
					Containment first = Ax.first(list);
					if (first == null) {
						return;
					}
					// note the [Lvl:0:f:f] run omissions are just the main body
					// run being split to cover tables - makes sense
					if (first.selection instanceof NonIndentContainer) {
						logger.debug(
								"Omit [non-critical, just omitting indentrun decorator]: {}",
								ct.selection.get().startContainingNode().ntc());
						ct.selection.setOmit(true);
					}
				});
	}

	void normalise(IndentNormalisation norm, IndentRun run) {
		Containment containment = indentRuns.getLowestContainment(run, true);
		if (containment.selection != run) {
			// this will be an intermediate, ignore
			return;
		}
		if (!norm.flattenAllMultipleLevelIndents
				&& !norm.begin.matches(containment.typedSelection())) {
			return;
		}
		List<Containment> wrappingContainments = new ArrayList<>();
		Containment highestWrappingSameRange = containment;
		Containment cursor = containment.parent();
		/*
		 * @formatter:off
		 * a multiple block jump down is controlled by the start of the containment ... e.g
		 * 
		 * S1
		 *   S2
		 *     L1
		 *     L2
		 * 
		 * where only L1, L2 have content - S1, S2 are 'synthetic' - block containers
		 * 
		 * @formatter:on
		 */
		while (cursor != null && Objects.equals(cursor.selection.get().start,
				containment.selection.get().start)) {
			wrappingContainments.add(cursor);
			highestWrappingSameRange = cursor;
			cursor = cursor.parent();
		}
		boolean omit = true;
		// heuristics
		int containmentReductionLimit = peer
				.getContainmentReductionLimit(wrappingContainments, run);
		if (norm.flattenAllMultipleLevelIndents) {
			omit = true;
		} else if (norm.end
				.matches(highestWrappingSameRange.typedSelection())) {
			omit = true;
			containmentReductionLimit--;
		}
		if (omit && containmentReductionLimit > 0) {
			wrappingContainments.stream().limit(containmentReductionLimit)
					.forEach(intermediate -> intermediate.selection
							.setOmit(true));
		}
	}

	public static class IndentNormalisation {
		static class NormalisationElement {
			int level;

			boolean list;

			NormalisationElement(int level, boolean list) {
				this.level = level;
				this.list = list;
			}

			public boolean matches(IndentRun selection) {
				return level == selection.logicalDepth
						&& list == selection.list;
			}
		}

		NormalisationElement begin;

		NormalisationElement end;

		boolean flattenAllMultipleLevelIndents;

		public IndentNormalisation(boolean flattenAllMultipleLevelIndents) {
			this.flattenAllMultipleLevelIndents = flattenAllMultipleLevelIndents;
		}

		public IndentNormalisation(int beginLevel, boolean beginList,
				int endLevel, boolean endList) {
			begin = new NormalisationElement(beginLevel, beginList);
			end = new NormalisationElement(endLevel, endList);
		}
	}

	public static class Peer {
		public interface Has {
			Peer getOutputIndentNormalisationPeer();
		}

		public List<IndentNormalisation> getIndentNormalisations() {
			return List.of();
		}

		public int getContainmentReductionLimit(
				List<Containment> wrappingContainments, IndentRun run) {
			return 0;
		}

		public static class NormaliseThreeToOne extends Peer {
			@Override
			public List<IndentNormalisation> getIndentNormalisations() {
				return List.of(new IndentNormalisation(1, true, 3, true));
			}
		}

		public static class NormaliseAllMultipleIndents extends Peer {
			@Override
			public List<IndentNormalisation> getIndentNormalisations() {
				return List.of(new IndentNormalisation(true));
			}

			@Override
			public int getContainmentReductionLimit(
					List<Containment> wrappingContainments, IndentRun run) {
				return wrappingContainments.size();
			}
		}
	}
}