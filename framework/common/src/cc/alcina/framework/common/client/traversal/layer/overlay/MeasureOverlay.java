package cc.alcina.framework.common.client.traversal.layer.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.behavior.ElementOffsetsRequired;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.SplitResult;
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

#2

The highlighter can either work by wrapping texts, or by  wrapping the _entire_ range, 
potentially splitting boundary nodes


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

		default DomNode getContainingBlock(DomNode node) {
			return node.ancestors().orSelf().match(this::isBlock).orElse(null);
		}

		boolean isSegmentBoundary(DomNode containingNode);

		/*
		 * i.e. -not- a TR
		 */
		boolean allowsArbitraryInsert(DomNode node);
	}

	public enum Type {
		HIGHLIGHT, WRAP, ENDPOINTS
	}

	public interface Wrapper {
		void wrap(Location.Range range);
	}

	public interface Highlighter {
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

		DomNode highlight(DomNode node);

		boolean isHighlit(DomNode node);
	}

	public static class Endpoints {
		public DomNode start;

		public DomNode end;
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
		public static DocumentElement of(MeasureSelection selection) {
			Measure measure = selection.get().startContainingNode().document
					.getDocumentElementNode().asRange()
					.toMeasure(DocumentElementToken.TYPE);
			return new DocumentElement(selection, measure);
		}

		private DocumentElement(MeasureSelection parent, Measure measure) {
			super(parent, measure);
		}
	}

	public Location.Range initialRange;

	public Location.Range splitRange;

	public Location.Range extendedRange;

	StyleResolver styleResolver;

	public List<Measure> overlays = new ArrayList<>();

	public Highlighter highlighter;

	public Wrapper wrapper;

	public boolean parentRelativeFixed;

	/**
	 * The overlay (decoration of the measure) depends on the type
	 */
	public Type type = Type.HIGHLIGHT;

	DomNode positioningElement;

	public Endpoints endpoints;

	DepthStrategy depthStrategy;

	/**
	 * How should the overlay split its boundary points?
	 */
	public enum DepthStrategy {
		shallowest {
			@Override
			Range adjustLocationDepths(Range range,
					StyleResolver styleResolver) {
				return range.toShallowestNodes();
			}
		},
		shallowest_insertable {
			@Override
			Range adjustLocationDepths(Range range,
					StyleResolver styleResolver) {
				return range.toShallowestNodes(n -> n.isText()
						|| styleResolver.allowsArbitraryInsert(n));
			}
		},
		deepest_non_block {
			@Override
			Range adjustLocationDepths(Range range,
					StyleResolver styleResolver) {
				Range deepestStartEndNode = range.toDeepestStartEndNode();
				if (deepestStartEndNode.isSingleNode()) {
					return deepestStartEndNode;
				}
				Range adjusted = range
						.toShallowestNodes(n -> !styleResolver.isBlock(n));
				return adjusted;
			}

			Range splitIfNecessary(Range range, StyleResolver styleResolver) {
				Location start = range.start;
				Location end = range.end;
				if (start.getContainingNode().parent() != end
						.getContainingNode().parent()) {
					DomNode commonContainer = range.getCommonContainingNode();
					Preconditions.checkState(commonContainer.descendants()
							.noneMatch(n -> styleResolver.isBlock(n)));
					start = commonContainer.split(start);
					end.getIndex();
					end = commonContainer.split(end);
					return new Range(start, end);
				} else {
					return super.splitIfNecessary(range, styleResolver);
				}
			}
		};

		abstract Range adjustLocationDepths(Range truncateAbsolute,
				StyleResolver styleResolver);

		Range splitIfNecessary(Range range, StyleResolver styleResolver) {
			Location start = range.start;
			Location end = range.end;
			if (!start.isAtNodeBoundary()) {
				SplitResult split = start.split();
				start = split.after.asLocation();
				end.getIndex();
			}
			if (!end.isAtNodeBoundary()) {
				SplitResult split = end.split();
				end = split.contents.asRange().end;
			}
			return new Range(start, end);
		}
	}

	/**
	 * All parameters are not *always* required, but beyond the simplest case
	 * (split shallowest/text nodes), the instance will need.a styleResolver
	 */
	public MeasureOverlay(StyleResolver styleResolver, DomDocument document,
			IntPair textRange, DepthStrategy depthStrategy) {
		this.styleResolver = styleResolver;
		this.depthStrategy = depthStrategy;
		Range range = document.getDocumentElementNode().asRange()
				.truncateAbsolute(textRange.i1, textRange.i2);
		this.initialRange = depthStrategy.adjustLocationDepths(range,
				styleResolver);
	}

	public void detach() {
		overlays.stream().filter(Measure::isAttached)
				.forEach(overlay -> overlay.startContainingNode().strip());
	}

	public ExtendResult extend(BoundaryTraversals quota, boolean reversed) {
		return new BoundaryParser(this).extend(quota, reversed);
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
		switch (type) {
		case WRAP:
			wrapper.wrap(ensureSplitRange());
			break;
		case HIGHLIGHT:
			highlightTexts();
			break;
		case ENDPOINTS:
			markEndpoints();
			break;
		default:
			throw new UnsupportedOperationException();
		}
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

	void markEndpoints() {
		Range range = ensureSplitRange();
		if (endpoints.start != null) {
			DomNode containingNode = range.start.getContainingNode();
			if (containingNode.isText()) {
				containingNode.relative().insertBeforeThis(endpoints.start);
			} else {
				containingNode.children.insertAsFirstChild(endpoints.start);
			}
		}
		if (endpoints.end != null) {
			DomNode containingNode = range.end.getContainingNode();
			if (containingNode.isText()) {
				containingNode.relative().insertAfterThis(endpoints.end);
			} else {
				containingNode.children.append(endpoints.end);
			}
		}
	}

	void highlightTexts() {
		/*
		 * add the first text (if any) for accurate positioining
		 */
		containedTexts().stream().map(node -> {
			com.google.gwt.dom.client.Node parentNode = node.gwtNode()
					.getParentNode();
			if (parentNode instanceof Element.RestrictedElementContent) {
				return null;
			}
			Measure measure = null;
			if (highlighter != null) {
				DomNode highlit = highlighter.highlight(node);
				measure = Measure.fromNode(highlit, HighlightToken.TYPE)
						.withData(highlighter);
			} else {
				measure = Measure.fromNode(node.asDomNode(), GenericToken.TYPE);
			}
			return measure;
		}).filter(Objects::nonNull).forEach(overlays::add);
	}

	Range ensureSplitRange() {
		if (splitRange == null) {
			splitRange = depthStrategy.splitIfNecessary(initialRange,
					styleResolver);
		}
		return splitRange;
	}

	List<DomNode> containedTexts() {
		Range range = ensureSplitRange();
		List<DomNode> result = new ArrayList<>();
		Location cursor = range.start;
		while (cursor.getIndex() < range.end.getIndex()) {
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
}
