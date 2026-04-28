package cc.alcina.framework.common.client.traversal.layer.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.behavior.ElementOffsetsRequired;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeText.SplitResult;
import cc.alcina.framework.common.client.dom.DomNodeBuilder;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.DocumentElementToken;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.overlay.BoundaryParser.ExtendResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;

/* 
@formatter:off
WIP

This is an attempt to bridge DomNode and MeasureContainment structures, for 
mutating measures. Behaviours are:

- The overlay begins with a measure
- The measure can be extended
- The measure can be 'applied' - modifying the dom in various ways

Points are:

- How do overlapping overlays of the same type interact?
- How do overlapping overlays of different types interact?
- Particularly when merge/split happens out of order?
- What are the persistence requirements?

Api weakness:

- Measure is really "range + meaning" - is it really appropriate for this? Because it's really "range + overlay"
... ahh, no, it's not the inputs that are measures, it's the outputs. Gotcha

 * @formatter:on
 */
public class MeasureOverlay {
	public interface Has {
		MeasureOverlay provideMeasureOverlay();
	}

	public static class HighlightToken
			implements Measure.Token.Typed<Highlighter> {
		public static HighlightToken TYPE = new HighlightToken();

		private HighlightToken() {
		}
	}

	public static class GenericToken implements Measure.Token {
		public static GenericToken TYPE = new GenericToken();

		private GenericToken() {
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

	public List<Measure> overlays = new ArrayList<>();

	public Highlighter highlighter;

	public boolean parentRelativeFixed;

	DomNode positioningElement;

	public interface Highlighter {
		DomNode highlight(DomNode node);

		public static class TagClassName implements Highlighter {
			String tagName;

			String className;

			public TagClassName(String tagName, String className) {
				this.tagName = tagName;
				this.className = className;
			}

			@Override
			public DomNode highlight(DomNode node) {
				DomNodeBuilder builder = node.builder().tag(tagName);
				if (Ax.notBlank(className)) {
					builder.className(className);
				}
				return builder.wrap();
			}

			@Override
			public boolean isHighlit(DomNode node) {
				return node.tagAndClassIs(tagName, className);
			}
		}

		boolean isHighlit(DomNode node);
	}

	public void detach() {
		overlays.stream().filter(Measure::isAttached)
				.forEach(overlay -> overlay.containingNode().strip());
	}

	public ExtendResult extend(BoundaryTraversals quota, boolean reversed) {
		return new BoundaryParser(this).extend(quota, reversed);
	}

	public MeasureOverlay(StyleResolver styleResolver, DomDocument document,
			IntPair textRange) {
		this.styleResolver = styleResolver;
		this.initialRange = document.getDocumentElementNode().asRange()
				.truncateAbsolute(textRange.i1, textRange.i2)
				.toShallowestNodes();
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

	public void attach() {
		if (highlighter != null) {
			containedTexts().stream().map(node -> {
				com.google.gwt.dom.client.Node parentNode = node.gwtNode()
						.getParentNode();
				if (parentNode instanceof Element.RestrictedElementContent) {
					return null;
				}
				DomNode highlit = highlighter.highlight(node);
				Measure measure = Measure.fromNode(highlit, HighlightToken.TYPE)
						.withData(highlighter);
				return measure;
			}).filter(Objects::nonNull).forEach(overlays::add);
		} else {
			/*
			 * add the first text (if any) for accurate positioining
			 */
			containedTexts().stream().map(node -> {
				com.google.gwt.dom.client.Node parentNode = node.gwtNode()
						.getParentNode();
				if (parentNode instanceof Element.RestrictedElementContent) {
					return null;
				}
				Measure measure = Measure.fromNode(node.asDomNode(),
						GenericToken.TYPE);
				return measure;
			}).filter(Objects::nonNull).findFirst().ifPresent(overlays::add);
		}
	}

	List<DomNode> containedTexts() {
		Location start = initialRange.start;
		Location end = initialRange.end;
		if (!start.isAtNodeBoundary()) {
			SplitResult split = start.split();
			start = split.after.asLocation();
			end.getIndex();
		}
		if (!end.isAtNodeBoundary()) {
			SplitResult split = end.split();
			end = split.after.asLocation();
		}
		List<DomNode> result = new ArrayList<>();
		Location cursor = start;
		while (cursor.getIndex() < end.getIndex()) {
			if (cursor.isTextNode() && cursor.isAtNodeStart()) {
				result.add(cursor.getContainingNode());
			}
			cursor = cursor.relativeLocation(RelativeDirection.NEXT_LOCATION,
					TextTraversal.EXIT_NODE);
		}
		return result;
	}

	Element getElement() {
		return getPositioningElement().gwtElement();
	}

	public int getTop() {
		return getElement().getAbsoluteTop();
	}

	/*
	 * positioning element should be cached, since it may need an associated
	 * behavior attached on eval
	 */
	public DomNode getPositioningElement() {
		if (positioningElement != null) {
			return positioningElement;
		}
		Location loc = initialRange.start;
		if (overlays.size() > 0) {
			loc = overlays.get(0).start;
		}
		if (loc.isAtNodeEnd()) {
			loc = loc.relativeLocation(RelativeDirection.NEXT_DOMNODE_START);
		}
		positioningElement = loc.getContainingNode().ancestors()
				.selfOrContainingElement();
		Element gwtElement = positioningElement.gwtElement();
		ElementOffsetsRequired offsetsBehavior = getOffsetsBehavior();
		if (!gwtElement.hasBehavior(offsetsBehavior.getClass())) {
			gwtElement.addBehavior(offsetsBehavior);
		}
		return positioningElement;
	}

	public ElementOffsetsRequired getOffsetsBehavior() {
		return parentRelativeFixed
				? ElementOffsetsRequired.ParentRelativeFixed.INSTANCE
				: ElementOffsetsRequired.INSTANCE;
	}
}
