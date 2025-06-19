package cc.alcina.framework.common.client.traversal.layer.overlay;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.DocumentElementToken;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.overlay.BoundaryParser.ExtendResult;

/* 
@formatter:off
WIP

This is an attempt to bridge DomNode and MeasureContainment structures, for 
mutating measures. Behaviours are:

- The overlay begins with a measure
- The measure can be extended
- The measure can be 'applied' - modifying the dom in various ways

Points are:

- How do overlapping overlays of the same time interact?
- How do overlapping overlays of different types interact?
- Particularly when merge/split happens out of order?
- What are the persistence requirements?

 * @formatter:on
 */
public class MeasureOverlay {
	public static class HighlightToken implements Measure.Token {
		public static HighlightToken TYPE = new HighlightToken();

		private HighlightToken() {
		}
	}

	public static class ExtractToken implements Measure.Token {
		public static ExtractToken TYPE = new ExtractToken();

		private ExtractToken() {
		}
	}

	public static class ExtendToken implements Measure.Token {
		public static ExtendToken TYPE = new ExtendToken();

		private ExtendToken() {
		}
	}

	public static class ExtendedToken implements Measure.Token {
		public static ExtendedToken TYPE = new ExtendedToken();

		private ExtendedToken() {
		}
	}

	/**
	 * Provide style information for block traversal
	 */
	public interface StyleResolver {
		boolean isBlock(DomNode node);

		boolean isSegmentBoundary(DomNode containingNode);
	}

	/**
	 * A measure to be extended
	 */
	static class ExtendMeasureSelection extends MeasureSelection {
		BoundaryTraversals quota;

		boolean forwards;

		public ExtendMeasureSelection(Node parentNode, Measure measure,
				BoundaryTraversals quota, boolean forwards) {
			super(parentNode, measure);
			this.quota = quota;
			this.forwards = forwards;
		}
	}

	/*
	 * A wrapping selection for the document
	 */
	static class DocumentElement extends MeasureSelection {
		private DocumentElement(MeasureSelection parent, Measure measure) {
			super(parent, measure);
		}

		public static DocumentElement of(MeasureSelection selection) {
			Measure measure = selection.get().containingNode().document
					.getDocumentElementNode().asRange()
					.toMeasure(DocumentElementToken.TYPE);
			return new DocumentElement(selection, measure);
		}
	}

	public Location.Range initialRange;

	public Location.Range extendedRange;

	StyleResolver styleResolver;

	public ExtendResult extend(BoundaryTraversals quota, boolean reversed) {
		return new BoundaryParser(this).extend(quota, reversed);
	}

	public MeasureOverlay(StyleResolver styleResolver,
			Location.Range initialRange) {
		this.styleResolver = styleResolver;
		this.initialRange = initialRange;
	}

	public void mergeExtensionRange(Range range) {
		if (range == null) {
			return;
		}
		if (extendedRange == null) {
			extendedRange = initialRange.clone();
		}
		if (range.start.compareTo(extendedRange.start) < 0) {
			extendedRange = new Range(range.start, extendedRange.end);
		}
		if (range.end.compareTo(extendedRange.end) > 0) {
			extendedRange = new Range(extendedRange.start, range.end);
		}
	}
}
