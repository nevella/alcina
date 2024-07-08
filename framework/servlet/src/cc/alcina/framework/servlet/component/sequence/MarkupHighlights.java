package cc.alcina.framework.servlet.component.sequence;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Renders some DOM with highlights + scroll to
 * <ul>
 * <li>Initializer can take plain text (it'll be wrapped in a pre)
 * </ul>
 */
public class MarkupHighlights extends Model.All {
	private static final String MARKUP_HIGHLIGHT = "__markup_highlight";

	private static final String HIGHLIGHT_RANGE = "__highlight_range";

	Model highlitMarkup;

	private DomDocument doc;

	private List<IntPair> ranges;

	public MarkupHighlights(String content, boolean markup,
			List<IntPair> ranges) {
		this.ranges = ranges;
		if (markup) {
			doc = DomDocument.from(content);
		} else {
			doc = DomDocument.from("<pre><code/></pre>");
			doc.getDocumentElementNode().children.firstElement()
					.setText(content);
		}
		doc.setReadonly(true);
		ranges.forEach(this::wrap);
		highlitMarkup = new LeafModel.TagMarkup("div", doc.fullToString());
	}

	class HighlightedRange {
		int idx;
	}

	void wrap(IntPair numericRange) {
		DomNode domNode = doc.getDocumentElementNode();
		// not strictly true, but mutation invalidation is handled manually
		Range range = domNode.asRange();
		// split the boundary text nodes
		Range truncated = range.truncateAbsolute(numericRange.i1,
				numericRange.i2);
		truncated.start.toTextLocation(true).ensureAtBoundary();
		doc.invalidateLocations();
		range = domNode.asRange();
		truncated = range.truncateAbsolute(numericRange.i1, numericRange.i2);
		truncated.end.toTextLocation(true).ensureAtBoundary();
		// manually invalidate
		doc.invalidateLocations();
		List<DomNode> wrap = domNode.stream().filter(n -> {
			int index = n.asDomNode().asLocation().index;
			return n.isText() && index >= numericRange.i1
					&& index < numericRange.i2;
		}).collect(Collectors.toList());
		List<DomNode> wrapped = wrap.stream().map(
				t -> t.builder().tag("span").className(MARKUP_HIGHLIGHT).wrap())
				.collect(Collectors.toList());
		wrapped.get(0).setAttr(HIGHLIGHT_RANGE, numericRange.toString());
	}

	public void goToRange(int rangeIdx) {
		if (!highlitMarkup.provideIsBound()) {
			return;// multiple renders
		}
		String matchTerm = ranges.get(rangeIdx).toString();
		Element matchElement = highlitMarkup.provideElement().asDomNode()
				.stream().filter(n -> n.attrIs(HIGHLIGHT_RANGE, matchTerm))
				.findFirst().get().gwtElement();
		matchElement.scrollIntoView();
	}
}
