package cc.alcina.framework.servlet.component.traversal;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HtmlParser;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.environment.style.StyleScoper;

/**
 * SelectionMarkup implementation for a single document (naive, only used in
 * RecipeParser)
 */
public class SelectionMarkupSingle extends SelectionMarkup {
	final SelectionTraversal traversal;

	final String baseCss;

	final IsBlock isBlock;

	/*
	 * max chars to either side of the selection
	 */
	public int maxContextChars = 2000;

	/*
	 * max blocks to either side of the selection
	 */
	public int maxContextBlocks = 2;

	public SelectionMarkupSingle(SelectionTraversal traversal, String baseCss,
			IsBlock isBlock) {
		this.traversal = traversal;
		this.baseCss = baseCss;
		this.isBlock = isBlock;
	}

	@Override
	protected String getCss(Query query) {
		String css = Registry.impl(StyleScoper.class).scope(baseCss,
				query.styleScope);
		return css;
	}

	@Override
	protected Model getModel(Query query) {
		Model model = null;
		try {
			model = new ModelBuilder(query).model;
		} catch (Exception e) {
			model = new LeafModel.TextTitle(
					Ax.format("Selection markup exception: %s",
							CommonUtils.toSimpleExceptionMessage(e)));
		}
		return model;
	}

	class ModelBuilder {
		Query query;

		Model model;

		Measure selectionMeasure;

		MeasureSelection measureSelection;

		Location.Range expandedMeasure;

		class MeasureExpander {
			Location start;

			Location end;

			public Location.Range expand() {
				start = extend(selectionMeasure.start, false);
				end = extend(selectionMeasure.end, true);
				return new Location.Range(start, end);
			}

			Location extend(Location from, boolean forwards) {
				Location cursor = from;
				int blockCount = 0;
				while (!cursor.isAtDocumentStart()
						&& !cursor.isAtDocumentEnd()) {
					int charCount = Math
							.abs(cursor.getIndex() - from.getIndex());
					if (charCount > maxContextChars) {
						break;
					}
					DomNode containingNode = cursor.getContainingNode();
					if (cursor.isAtNodeStart() && containingNode.isElement()
							&& isBlock.isBlock(containingNode)) {
						blockCount++;
						if (blockCount >= maxContextBlocks) {
							break;
						}
					}
					cursor = cursor.relativeLocation(
							forwards ? RelativeDirection.NEXT_LOCATION
									: RelativeDirection.PREVIOUS_LOCATION,
							TextTraversal.TO_END_OF_NODE);
				}
				return cursor;
			}
		}

		ModelBuilder(Query query) {
			this.query = query;
			computeMeasure();
			if (selectionMeasure == null) {
				return;
			}
			expandMeasure();
			generateModel();
		}

		void expandMeasure() {
			expandedMeasure = new MeasureExpander().expand();
		}

		void computeMeasure() {
			if (query.selection instanceof MeasureSelection) {
				measureSelection = (MeasureSelection) query.selection;
				selectionMeasure = new RangeSelectionSequence(traversal, query)
						.ioSelection();
			} else {
				return;
			}
		}

		void generateModel() {
			String markup = XmlUtils
					.removeXmlDeclaration(expandedMeasure.markup());
			Element parsed = HtmlParser.parseMarkup(markup);
			DomNode domNode = parsed.asDomNode();
			// not strictly true, but mutation invalidation is handled manually
			domNode.document.setReadonly(true);
			Range range = domNode.asRange();
			Range truncated = range.truncateAbsolute(
					selectionMeasure.start.getIndex()
							- expandedMeasure.start.getIndex(),
					selectionMeasure.end.getIndex()
							- expandedMeasure.start.getIndex());
			truncated.start.toTextLocation(true).ensureAtBoundary();
			truncated.end.toTextLocation(true).ensureAtBoundary();
			domNode.document.invalidateLocations();
			List<DomNode> wrap = domNode.stream().filter(n -> {
				int index = n.asDomNode().asLocation().getIndex();
				return n.isText() && index >= truncated.start.getIndex()
						&& index < truncated.end.getIndex();
			}).collect(Collectors.toList());
			wrap.forEach(t -> t.builder().tag("span")
					.className("__traversal_markup_selected").wrap());
			markup = parsed.getInnerHTML();
			this.model = new LeafModel.TagMarkup("div", markup);
		}
	}

	/*
	 * Used for context scoping, identifies nodes as block/non-block
	 */
	public interface IsBlock {
		boolean isBlock(DomNode node);
	}
}
