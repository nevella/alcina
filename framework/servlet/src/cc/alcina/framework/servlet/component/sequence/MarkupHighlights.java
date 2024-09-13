package cc.alcina.framework.servlet.component.sequence;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * Renders some DOM with highlights + scroll to
 * <ul>
 * <li>Initializer can take plain text (it'll be wrapped in a pre)
 * </ul>
 */
public class MarkupHighlights extends Model.Fields {
	private static final String MARKUP_HIGHLIGHT = "__markup_highlight";

	private static final String SELECTED_MARKUP_HIGHLIGHT = "__selected_markup_highlight";

	@Directed
	LeafModel.TagMarkup highlitMarkup;

	List<WrappedRange> wrappedRanges;

	WrappedRange selected;

	List<IntPair> numericRanges;

	int goToRangeIndex;

	public MarkupHighlights(String content, boolean markup,
			List<IntPair> numericRanges, int goToRangeIndex) {
		this.numericRanges = numericRanges;
		this.goToRangeIndex = goToRangeIndex;
		Document.get().asDomNode().document.setReadonly(true);
		if (markup) {
		} else {
			DomDocument doc = DomDocument.from("<pre><code/></pre>");
			doc.getDocumentElementNode().children.firstElement()
					.setText(content);
			content = doc.fullToString();
		}
		highlitMarkup = new LeafModel.TagMarkup("div", content);
	}

	void wrapRangesAndGo() {
		wrappedRanges = numericRanges.stream().map(WrappedRange::new)
				.collect(Collectors.toList());
		wrappedRanges.forEach(WrappedRange::wrap);
		goToRange(goToRangeIndex);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			Client.eventBus().queued().lambda(this::wrapRangesAndGo).dispatch();
		}
	}

	class HighlightedRange {
		int idx;
	}

	class WrappedRange {
		IntPair numericRange;

		List<DomNode> wrapped;

		WrappedRange(IntPair range) {
			this.numericRange = range;
		}

		void wrap() {
			if (!highlitMarkup.provideIsBound()) {
				return;
			}
			DomNode domNode = highlitMarkup.provideElement().asDomNode();
			// not strictly true, but mutation invalidation is handled manually
			Range range = domNode.asRange();
			domNode.document.invalidateLocations();
			// split the boundary text nodes
			Range truncated = range.truncateRelative(numericRange.i1,
					numericRange.i2);
			truncated.start.toTextLocation(true).ensureAtBoundary();
			range = domNode.asRange();
			truncated = range.truncateRelative(numericRange.i1,
					numericRange.i2);
			domNode.document.invalidateLocations();
			range = domNode.asRange();
			truncated.end.toTextLocation(true).ensureAtBoundary();
			domNode.document.invalidateLocations();
			List<DomNode> wrap = domNode.stream().filter(n -> {
				int index = n.asDomNode().asLocation().index
						- domNode.asLocation().index;
				return n.isText() && index >= numericRange.i1
						&& index < numericRange.i2;
			}).collect(Collectors.toList());
			wrapped = wrap.stream()
					.map(t -> t.builder().tag("span")
							.className(MARKUP_HIGHLIGHT).wrap())
					.collect(Collectors.toList());
		}

		void scrollTo() {
			wrapped.get(0).gwtElement().scrollIntoView();
		}

		void setSelected(boolean selected) {
			wrapped.forEach(node -> node.css()
					.putClass(SELECTED_MARKUP_HIGHLIGHT, selected));
		}
	}

	void goToRange(int rangeIdx) {
		if (provideIsUnbound() || rangeIdx < 0
				|| rangeIdx >= wrappedRanges.size()) {
			return;
		}
		if (selected != null) {
			selected.setSelected(false);
		}
		selected = wrappedRanges.get(rangeIdx);
		selected.scrollTo();
		selected.setSelected(true);
	}
}
