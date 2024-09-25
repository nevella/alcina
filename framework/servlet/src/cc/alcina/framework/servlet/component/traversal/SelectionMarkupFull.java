package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * SelectionMarkup implementation for a single document
 */
public class SelectionMarkupFull extends SelectionMarkup {
	final SelectionTraversal traversal;

	public SelectionMarkupFull(SelectionTraversal traversal) {
		this.traversal = traversal;
	}

	@Override
	protected String getCss(Query query) {
		return "";
	}

	ToMarkupHighlights toMarkupHighlights;

	@Override
	protected Model getModel(Query query) {
		if (toMarkupHighlights == null) {
			try {
				toMarkupHighlights = new ToMarkupHighlights();
				toMarkupHighlights.renderDocuments(query);
			} catch (Exception e) {
				return new LeafModel.TextTitle(
						Ax.format("Selection markup exception: %s",
								CommonUtils.toSimpleExceptionMessage(e)));
			}
		}
		toMarkupHighlights.highlight(query);
		return toMarkupHighlights.getModel(query);
	}

	class ToMarkupHighlights {
		VariantHighlights input;

		VariantHighlights output;

		RangeSelectionSequence rangeSelectionSequence;

		void renderDocuments(Query query) {
			updateSequence(query);
			input = new VariantHighlights(true);
			output = new VariantHighlights(false);
		}

		private void updateSequence(Query query) {
			rangeSelectionSequence = new RangeSelectionSequence(traversal,
					query);
		}

		class VariantHighlights {
			MarkupHighlights markupHighlights;

			DomNode contentsNode;

			boolean input;

			VariantHighlights(boolean input) {
				this.input = input;
				DomNode containingNode = getContainingNode(
						rangeSelectionSequence.getRange(input));
				if (containingNode != null) {
					DomNode body = containingNode.document.html().body();
					contentsNode = body != null ? body
							: containingNode.document.getDocumentElementNode();
					String markup = contentsNode.fullToString();
					this.markupHighlights = new MarkupHighlights(markup, true,
							List.of(), -1);
				} else {
				}
			}

			DomNode getContainingNode(Selection.WithRange withRangeSelection) {
				if (withRangeSelection != null
						&& withRangeSelection.provideRange() != null
						&& withRangeSelection.provideRange()
								.containingNode() != null) {
					return withRangeSelection.provideRange().containingNode();
				} else {
					return null;
				}
			}

			void highlight(Query query) {
				if (contentsNode == null) {
					return;
				}
				DomNode containingNode = getContainingNode(
						rangeSelectionSequence.getRange(this.input));
				List<IntPair> pairs = null;
				if (containingNode == null
						|| !containingNode.isAttachedToDocument()
						|| !contentsNode.asRange()
								.contains(containingNode.asRange())) {
					pairs = List.of();
				} else {
					IntPair pair = containingNode.asRange().toIntPair();
					pair = pair.shiftRight(-contentsNode.asLocation().index);
					pairs = List.of(pair);
				}
				markupHighlights.putRanges(pairs, 0);
				markupHighlights.wrapRangesAndGo();
			}
		}

		void highlight(Query query) {
			updateSequence(query);
			input.highlight(query);
			output.highlight(query);
		}

		VariantHighlights getHighlights(Query query) {
			return query.input ? input : output;
		}

		Model getModel(Query query) {
			return getHighlights(query).markupHighlights;
		}
	}

	/*
	 * Used for context scoping, identifies nodes as block/non-block
	 */
	public interface IsBlock {
		boolean isBlock(DomNode node);
	}
}
