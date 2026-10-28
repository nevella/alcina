package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.html.LeafBlockLayer.LeafBlock;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.Containment;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.MeasureTreeComparator;

/**
 * <p>
 * Omit ignoreable children such as LeafBlocks in tr/tbody, or listitemlabel
 */
public class MeasureComputeStructuredHtmlIgnores
		extends Layer<DocumentTransformerSelection> {
	@Override
	public void process(DocumentTransformerSelection selection)
			throws Exception {
		Order order = state.traversalContext(Order.Has.class).getOrder();
		Stream<MeasureSelection> filteredSelections = state.traversalState.selections
				.get(MeasureSelection.class, true).stream()
				.filter(m -> !m.isOmit()
						&& !(m instanceof MeasureSelection.IgnoreOverlaps));
		List<MeasureSelection> measures = filteredSelections
				.sorted(new MeasureTreeComparator(order))
				.collect(Collectors.toList());
		MeasureContainment measureContainment = new MeasureContainment(order,
				measures);
		measureContainment.containments()
				.filter(c -> c.selection instanceof LeafBlockLayer.LeafBlock)
				.forEach(b -> {
					LeafBlockLayer.LeafBlock leafBlock = (LeafBlock) b.selection;
					Containment parent = b.parent();
					if (parent != null) {
						// TABLE NODE/BLOCK
						if (parent.selection
								.get().token instanceof LayoutTokens.TextChildrenNotVisible) {
							leafBlock.setOmit(true);
						}
					}
				});
	}
}