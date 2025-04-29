package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.DocumentTransformationTraversal;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.WithRange;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.environment.style.StyleScoper;

/**
 * SelectionMarkup implementation for a single document
 * 
 */
public class SelectionMarkupFull extends SelectionMarkup {
	final SelectionTraversal traversal;

	public SelectionMarkupFull(SelectionTraversal traversal) {
		this.traversal = traversal;
	}

	@Override
	protected String getCss(Query query) {
		String documentCss = new DocumentTransformationTraversal(traversal)
				.getDocumentCss(query.input);
		String scopedCss = Registry.impl(StyleScoper.class).scope(documentCss,
				query.styleScope);
		return scopedCss;
	}

	protected String getContainerClassNames(boolean input) {
		return new DocumentTransformationTraversal(traversal)
				.getMarkupContainerClassnames(input);
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

	class ToMarkupHighlights implements ElementToSelection {
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
			query.elementToSelection = this;
		}

		class VariantHighlights {
			MarkupHighlights markupHighlights;

			DomNode contentsNode;

			boolean input;

			VariantHighlights(boolean input) {
				this.input = input;
				DomNode containingNode = getContainingNode(
						rangeSelectionSequence.getRange(input));
				DomNode body = containingNode.document.html().body();
				contentsNode = body != null ? body
						: containingNode.document.getDocumentElementNode();
				String markup = contentsNode.fullToString();
				markup = XmlUtils.removeNamespaceInfo(markup);
				markup = markup.replaceAll("(</?body)([^>]+>)", "$1-sub$2");
				this.markupHighlights = new MarkupHighlights(markup,
						getContainerClassNames(input), true, List.of(), -1);
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
				if (containingNode != null) {
					containingNode.document.invalidateLocations();
				}
				List<IntPair> pairs = null;
				if (rangeSelectionSequence.fullDocument
						|| containingNode == null
						|| !containingNode.isAttached() || !contentsNode
								.asRange().contains(containingNode.asRange())) {
					pairs = List.of();
				} else {
					IntPair pair = containingNode.asRange().toIntPair();
					pair = pair
							.shiftRight(-contentsNode.asLocation().getIndex());
					pairs = List.of(pair);
				}
				markupHighlights.updateRanges(pairs);
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

		@Override
		public Selection getSelection(Query query, Element container,
				Element source) {
			Range elementRange = source.asDomNode().asRange();
			VariantHighlights highlights = query.input ? input : output;
			Range containingRange = container.asDomNode().asRange();
			int originalDocOffset = highlights.contentsNode.asLocation()
					.getIndex();
			int originalDocIndex = elementRange.toIntPair().i1
					- containingRange.toIntPair().i1 + originalDocOffset;
			List<WithRange> matching = traversal.selections()
					.get(Selection.WithRange.class, true).stream()
					.filter(sel -> sel.provideRange() != null
							&& sel.provideRange().toIntPair()
									.contains(originalDocIndex))
					.filter(sel -> query.input ? true : this.isOutput(sel))
					.sorted(new Selection.WithRange.MostSpecificComparator())
					.toList();
			return Ax.first(matching);
		}

		boolean isOutput(Selection cursor) {
			Layer layer = traversal.layers().get(cursor);
			return layer.layerContext(Layer.Output.class) != null;
		}
	}

	/*
	 * Used for context scoping, identifies nodes as block/non-block
	 */
	public interface IsBlock {
		boolean isBlock(DomNode node);
	}

	void updateQuery(Query query) {
		toMarkupHighlights.highlight(query);
	}
}
